package com.locomotora.demo.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Contratos del panel de admin ("Actividad de usuarios"). Solo datos de cuenta y de actividad: nada de salud,
 * fotos ni contenido de los entrenamientos.
 */
public final class AdminDtos {
    private AdminDtos() {
    }

    public record StatsResponse(UserCounts users, UsageStats usage, TrainingStats training, List<DailyUsage> daily) {
    }

    /** Cuentas no borradas. {@code google}/{@code password}: forma de entrar (una cuenta puede tener las dos). */
    public record UserCounts(
            int total,
            int active,
            int disabled,
            int admins,
            int new7d,
            int new30d,
            int google,
            int password
    ) {
    }

    /**
     * Uso de la app (desde que existe el ping). {@code active*}: usuarios que la abrieron en las ultimas 24 h,
     * 7 y 30 dias. {@code averageSecondsPerUser30d}: entre los que la usaron en esos 30 dias.
     */
    public record UsageStats(
            int active24h,
            int active7d,
            int active30d,
            long seconds7d,
            long seconds30d,
            long averageSecondsPerUser30d
    ) {
    }

    /** Entrenamientos terminados de los ultimos 30 dias (tiene historial, no depende del ping). */
    public record TrainingStats(int workouts30d, long seconds30d) {
    }

    /** Un dia del grafico general: cuantos usuarios abrieron la app y cuanto tiempo en total. */
    public record DailyUsage(String date, int activeUsers, long seconds) {
    }

    /**
     * Una fila de la lista. {@code provider}: GOOGLE, PASSWORD o BOTH. {@code lastActivityAt}: ultimo ping o
     * ultimo entrenamiento, lo mas reciente (null si nunca).
     */
    public record UserSummary(
            String id,
            String name,
            String email,
            String status,
            String role,
            String provider,
            String createdAt,
            String lastActivityAt,
            long usageSeconds30d,
            int workouts30d
    ) {
    }

    public record UserPage(List<UserSummary> items, int page, int pageSize, long total) {
    }

    /** {@code canChangeStatus}: false para la propia cuenta y para otros admins. */
    public record UserDetail(
            UserSummary user,
            boolean onboardingCompleted,
            int routines,
            int workoutsTotal,
            long trainingSecondsTotal,
            long trainingSeconds30d,
            String lastWorkoutAt,
            int sessions30d,
            int aiRequests30d,
            List<UserDay> daily,
            List<AuditEntry> history,
            boolean canChangeStatus
    ) {
    }

    /** Un dia del grafico de un usuario (ultimos 30 dias, con los dias sin uso en 0). */
    public record UserDay(String date, long seconds, int sessions) {
    }

    /** Un cambio hecho por un admin sobre la cuenta. */
    public record AuditEntry(String action, String reason, String adminName, String createdAt) {
    }

    public record StatusRequest(
            @NotNull @Pattern(regexp = "ACTIVE|DISABLED") String status,
            @Size(max = 300) String reason
    ) {
    }
}
