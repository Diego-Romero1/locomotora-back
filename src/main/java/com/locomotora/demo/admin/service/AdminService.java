package com.locomotora.demo.admin.service;

import com.locomotora.demo.admin.dto.AdminDtos.DailyUsage;
import com.locomotora.demo.admin.dto.AdminDtos.StatsResponse;
import com.locomotora.demo.admin.dto.AdminDtos.StatusRequest;
import com.locomotora.demo.admin.dto.AdminDtos.TrainingStats;
import com.locomotora.demo.admin.dto.AdminDtos.UsageStats;
import com.locomotora.demo.admin.dto.AdminDtos.UserDay;
import com.locomotora.demo.admin.dto.AdminDtos.UserDetail;
import com.locomotora.demo.admin.dto.AdminDtos.UserPage;
import com.locomotora.demo.admin.dto.AdminDtos.UserSummary;
import com.locomotora.demo.admin.repository.AdminRepository;
import com.locomotora.demo.admin.repository.AdminRepository.AccountRow;
import com.locomotora.demo.admin.repository.AdminRepository.UserTraining;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.common.TimeZones;
import com.locomotora.demo.security.AccountStatusService;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Panel de admin: cifras generales, lista de usuarios con su actividad, detalle y habilitar/inhabilitar.
 * Ademas de la regla de SecurityConfig, cada metodo exige el rol ADMIN.
 */
@Service
public class AdminService {
    static final int CHART_DAYS = 14;
    static final int USER_DAYS = 30;
    private static final int HISTORY_LIMIT = 10;

    private final AdminRepository adminRepository;
    private final AccountStatusService accountStatusService;
    private final Clock clock;

    @Autowired
    public AdminService(AdminRepository adminRepository, AccountStatusService accountStatusService) {
        this(adminRepository, accountStatusService, Clock.systemUTC());
    }

    AdminService(AdminRepository adminRepository, AccountStatusService accountStatusService, Clock clock) {
        this.adminRepository = adminRepository;
        this.accountStatusService = accountStatusService;
        this.clock = clock;
    }

    public StatsResponse stats(String timeZone) {
        CurrentUser.requireAdmin();
        Instant now = clock.instant();
        LocalDate today = now.atZone(TimeZones.parse(timeZone)).toLocalDate();
        Instant since7d = now.minus(Duration.ofDays(7));
        Instant since30d = now.minus(Duration.ofDays(30));

        int[] active = adminRepository.activeUsers(now.minus(Duration.ofHours(24)), since7d, since30d);
        long[] usage = adminRepository.usageTotals(today.minusDays(6), today.minusDays(USER_DAYS - 1));
        long average = usage[2] == 0 ? 0 : usage[1] / usage[2];
        TrainingStats training = adminRepository.training(since30d);

        LocalDate chartFrom = today.minusDays(CHART_DAYS - 1);
        Map<LocalDate, long[]> byDay = adminRepository.dailyUsage(chartFrom);
        List<DailyUsage> daily = new ArrayList<>();
        for (LocalDate day = chartFrom; !day.isAfter(today); day = day.plusDays(1)) {
            long[] values = byDay.getOrDefault(day, new long[] {0, 0});
            daily.add(new DailyUsage(day.toString(), (int) values[0], values[1]));
        }

        return new StatsResponse(
                adminRepository.userCounts(since7d, since30d),
                new UsageStats(active[0], active[1], active[2], usage[0], usage[1], average),
                training,
                daily
        );
    }

    public UserPage users(String query, String status, int page, String timeZone) {
        CurrentUser.requireAdmin();
        String normalizedQuery = query == null || query.isBlank() ? null : query.trim();
        if (normalizedQuery != null && normalizedQuery.length() > 100) {
            normalizedQuery = normalizedQuery.substring(0, 100);
        }
        String normalizedStatus = normalizeStatusFilter(status);
        int safePage = Math.max(0, page);
        Instant now = clock.instant();
        LocalDate today = now.atZone(TimeZones.parse(timeZone)).toLocalDate();
        return new UserPage(
                adminRepository.listUsers(normalizedQuery, normalizedStatus, today.minusDays(USER_DAYS - 1),
                        now.minus(Duration.ofDays(USER_DAYS)), safePage),
                safePage,
                AdminRepository.PAGE_SIZE,
                adminRepository.countUsers(normalizedQuery, normalizedStatus)
        );
    }

    public UserDetail user(UUID userId, String timeZone) {
        UUID adminId = CurrentUser.requireAdmin();
        Instant now = clock.instant();
        LocalDate today = now.atZone(TimeZones.parse(timeZone)).toLocalDate();
        LocalDate from = today.minusDays(USER_DAYS - 1);
        Instant since30d = now.minus(Duration.ofDays(USER_DAYS));

        UserSummary summary = adminRepository.findUser(userId, from, since30d).orElseThrow(AdminService::notFound);
        UserTraining training = adminRepository.userTraining(userId, since30d);
        Map<LocalDate, long[]> byDay = adminRepository.userDailyUsage(userId, from);
        List<UserDay> daily = new ArrayList<>();
        int sessions = 0;
        for (LocalDate day = from; !day.isAfter(today); day = day.plusDays(1)) {
            long[] values = byDay.getOrDefault(day, new long[] {0, 0});
            daily.add(new UserDay(day.toString(), values[0], (int) values[1]));
            sessions += (int) values[1];
        }

        return new UserDetail(
                summary,
                adminRepository.onboardingCompleted(userId),
                adminRepository.activeRoutines(userId),
                training.workouts(),
                training.secondsTotal(),
                training.secondsRecent(),
                training.lastWorkoutAt() == null ? null : training.lastWorkoutAt().toString(),
                sessions,
                adminRepository.aiRequests(userId, since30d),
                daily,
                adminRepository.history(userId, HISTORY_LIMIT),
                canChangeStatus(adminId, userId, "ADMIN".equals(summary.role()))
        );
    }

    /**
     * Habilita o inhabilita una cuenta. No se puede cambiar la propia ni la de otro admin. La sesion de la cuenta
     * inhabilitada se corta en su proxima peticion (el filtro JWT deja de aceptar su token).
     */
    public UserDetail updateStatus(UUID userId, StatusRequest request, String timeZone) {
        UUID adminId = CurrentUser.requireAdmin();
        AccountRow account = adminRepository.findAccount(userId).orElseThrow(AdminService::notFound);
        if (adminId.equals(userId)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "You cannot change your own account");
        }
        if (account.isAdmin()) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Admin accounts cannot be changed");
        }
        String status = request.status();
        if (!status.equals(account.status())) {
            adminRepository.changeStatus(adminId, userId, status,
                    "DISABLED".equals(status) ? "DISABLE_USER" : "ENABLE_USER", normalizeReason(request.reason()));
            // Despues del commit: la proxima peticion de esa cuenta ya lee el estado nuevo.
            accountStatusService.evict(userId);
        }
        return user(userId, timeZone);
    }

    private static boolean canChangeStatus(UUID adminId, UUID userId, boolean targetIsAdmin) {
        return !adminId.equals(userId) && !targetIsAdmin;
    }

    private static String normalizeStatusFilter(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String upper = status.trim().toUpperCase(Locale.ROOT);
        if (!upper.equals("ACTIVE") && !upper.equals("DISABLED")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Invalid parameter: status");
        }
        return upper;
    }

    private static String normalizeReason(String reason) {
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    private static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, "User not found");
    }
}
