package com.locomotora.demo.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.locomotora.demo.admin.dto.AdminDtos.StatsResponse;
import com.locomotora.demo.admin.dto.AdminDtos.StatusRequest;
import com.locomotora.demo.admin.dto.AdminDtos.TrainingStats;
import com.locomotora.demo.admin.dto.AdminDtos.UserCounts;
import com.locomotora.demo.admin.dto.AdminDtos.UserDetail;
import com.locomotora.demo.admin.dto.AdminDtos.UserSummary;
import com.locomotora.demo.admin.repository.AdminRepository;
import com.locomotora.demo.admin.repository.AdminRepository.AccountRow;
import com.locomotora.demo.admin.repository.AdminRepository.UserTraining;
import com.locomotora.demo.common.ApiException;
import com.locomotora.demo.security.AccountStatusService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AdminServiceTest {
    private static final UUID ADMIN = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-00000000000b");
    private static final Instant NOW = Instant.parse("2026-11-24T15:00:00Z");

    private final AdminRepository repository = mock(AdminRepository.class);
    private final AccountStatusService accounts = mock(AccountStatusService.class);
    private final AdminService service = new AdminService(repository, accounts, Clock.fixed(NOW, ZoneOffset.UTC));

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void signedInAs(UUID id, String role) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                id.toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + role))));
    }

    private static HttpStatus statusOf(Throwable error) {
        return ((ApiException) error).status();
    }

    private void userDetailIsReadable(String status) {
        when(repository.findUser(eq(USER), any(), any())).thenReturn(Optional.of(new UserSummary(
                USER.toString(), "Ana", "ana@test.local", status, "USER", "PASSWORD",
                "2026-11-01T00:00:00Z", null, 0, 0)));
        when(repository.userTraining(eq(USER), any())).thenReturn(new UserTraining(0, 0, 0, null));
        when(repository.userDailyUsage(eq(USER), any())).thenReturn(Map.of());
        when(repository.history(eq(USER), anyInt())).thenReturn(List.of());
    }

    @Test
    void aRegularUserCannotUseThePanel() {
        signedInAs(USER, "USER");

        assertThatThrownBy(() -> service.stats("UTC"))
                .satisfies(error -> assertThat(statusOf(error)).isEqualTo(HttpStatus.FORBIDDEN));
        assertThatThrownBy(() -> service.updateStatus(ADMIN, new StatusRequest("DISABLED", null), "UTC"))
                .satisfies(error -> assertThat(statusOf(error)).isEqualTo(HttpStatus.FORBIDDEN));
        verify(repository, never()).changeStatus(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void theAdminCannotDisableTheirOwnAccount() {
        signedInAs(ADMIN, "ADMIN");
        when(repository.findAccount(ADMIN)).thenReturn(Optional.of(new AccountRow(ADMIN, "ACTIVE", "ADMIN")));

        assertThatThrownBy(() -> service.updateStatus(ADMIN, new StatusRequest("DISABLED", null), "UTC"))
                .satisfies(error -> assertThat(statusOf(error)).isEqualTo(HttpStatus.BAD_REQUEST));
        verify(repository, never()).changeStatus(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void anotherAdminCannotBeDisabled() {
        UUID otherAdmin = UUID.randomUUID();
        signedInAs(ADMIN, "ADMIN");
        when(repository.findAccount(otherAdmin)).thenReturn(Optional.of(new AccountRow(otherAdmin, "ACTIVE", "ADMIN")));

        assertThatThrownBy(() -> service.updateStatus(otherAdmin, new StatusRequest("DISABLED", null), "UTC"))
                .satisfies(error -> assertThat(statusOf(error)).isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void anUnknownUserIs404() {
        signedInAs(ADMIN, "ADMIN");
        when(repository.findAccount(USER)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateStatus(USER, new StatusRequest("DISABLED", null), "UTC"))
                .satisfies(error -> assertThat(statusOf(error)).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void disablingAUserIsAuditedAndCutsTheirSessionRightAway() {
        signedInAs(ADMIN, "ADMIN");
        when(repository.findAccount(USER)).thenReturn(Optional.of(new AccountRow(USER, "ACTIVE", "USER")));
        userDetailIsReadable("DISABLED");

        UserDetail detail = service.updateStatus(USER, new StatusRequest("DISABLED", "  spam  "), "UTC");

        InOrder order = inOrder(repository, accounts);
        order.verify(repository).changeStatus(ADMIN, USER, "DISABLED", "DISABLE_USER", "spam");
        order.verify(accounts).evict(USER);
        assertThat(detail.user().status()).isEqualTo("DISABLED");
        assertThat(detail.canChangeStatus()).isTrue();
    }

    @Test
    void enablingRecordsTheEnableAction() {
        signedInAs(ADMIN, "ADMIN");
        when(repository.findAccount(USER)).thenReturn(Optional.of(new AccountRow(USER, "DISABLED", "USER")));
        userDetailIsReadable("ACTIVE");

        service.updateStatus(USER, new StatusRequest("ACTIVE", " "), "UTC");

        verify(repository).changeStatus(ADMIN, USER, "ACTIVE", "ENABLE_USER", null);
    }

    @Test
    void settingTheSameStatusChangesNothing() {
        signedInAs(ADMIN, "ADMIN");
        when(repository.findAccount(USER)).thenReturn(Optional.of(new AccountRow(USER, "ACTIVE", "USER")));
        userDetailIsReadable("ACTIVE");

        service.updateStatus(USER, new StatusRequest("ACTIVE", null), "UTC");

        verify(repository, never()).changeStatus(any(), any(), anyString(), anyString(), any());
        verify(accounts, never()).evict(any());
    }

    @Test
    void theAdminSeesTheirOwnDetailButCannotChangeIt() {
        signedInAs(ADMIN, "ADMIN");
        when(repository.findUser(eq(ADMIN), any(), any())).thenReturn(Optional.of(new UserSummary(
                ADMIN.toString(), "Admin", "admin@test.local", "ACTIVE", "ADMIN", "GOOGLE", null, null, 0, 0)));
        when(repository.userTraining(eq(ADMIN), any())).thenReturn(new UserTraining(0, 0, 0, null));
        when(repository.userDailyUsage(eq(ADMIN), any())).thenReturn(Map.of());

        assertThat(service.user(ADMIN, "UTC").canChangeStatus()).isFalse();
    }

    @Test
    void theUserChartHas30DaysWithEmptyDaysAtZero() {
        signedInAs(ADMIN, "ADMIN");
        userDetailIsReadable("ACTIVE");
        when(repository.userDailyUsage(eq(USER), any()))
                .thenReturn(Map.of(LocalDate.parse("2026-11-24"), new long[] {600, 2}));

        UserDetail detail = service.user(USER, "UTC");

        assertThat(detail.daily()).hasSize(30);
        assertThat(detail.daily().get(0).date()).isEqualTo("2026-10-26");
        assertThat(detail.daily().get(29).seconds()).isEqualTo(600);
        assertThat(detail.sessions30d()).isEqualTo(2);
    }

    @Test
    void statsAverageTheUsageAmongUsersWhoUsedTheApp() {
        signedInAs(ADMIN, "ADMIN");
        when(repository.activeUsers(any(), any(), any())).thenReturn(new int[] {1, 2, 3});
        when(repository.usageTotals(any(), any())).thenReturn(new long[] {1200, 3600, 2});
        when(repository.training(any())).thenReturn(new TrainingStats(4, 14400));
        when(repository.dailyUsage(any())).thenReturn(Map.of());
        when(repository.userCounts(any(), any())).thenReturn(new UserCounts(5, 4, 1, 1, 1, 2, 3, 3));

        StatsResponse stats = service.stats("UTC");

        assertThat(stats.usage().averageSecondsPerUser30d()).isEqualTo(1800);
        assertThat(stats.daily()).hasSize(AdminService.CHART_DAYS);
        assertThat(stats.daily().get(AdminService.CHART_DAYS - 1).date()).isEqualTo("2026-11-24");
    }

    @Test
    void anInvalidStatusFilterIsRejected() {
        signedInAs(ADMIN, "ADMIN");

        assertThatThrownBy(() -> service.users(null, "DELETED", 0, "UTC"))
                .satisfies(error -> assertThat(statusOf(error)).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
