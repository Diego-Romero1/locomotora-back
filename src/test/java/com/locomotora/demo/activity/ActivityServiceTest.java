package com.locomotora.demo.activity;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** El servidor suma el tiempo entre pings seguidos; si pasaron mas de 2 minutos, empieza una sesion nueva. */
class ActivityServiceTest {
    private static final UUID USER = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final Instant NOW = Instant.parse("2026-11-24T03:30:00Z");

    private final ActivityRepository repository = mock(ActivityRepository.class);
    private final ActivityService service = new ActivityService(repository, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void theFirstPingStartsASessionWithoutTime() {
        when(repository.touchLastSeen(USER, NOW)).thenReturn(Optional.empty());

        service.record(USER, "UTC");

        verify(repository).addUsage(USER, LocalDate.parse("2026-11-24"), 0, true, NOW);
    }

    @Test
    void aPingAMinuteLaterAddsThatMinuteToTheSameSession() {
        when(repository.touchLastSeen(USER, NOW)).thenReturn(Optional.of(NOW.minusSeconds(60)));

        service.record(USER, "UTC");

        verify(repository).addUsage(USER, LocalDate.parse("2026-11-24"), 60, false, NOW);
    }

    @Test
    void aLongGapStartsANewSessionAndDoesNotCountTheGap() {
        when(repository.touchLastSeen(USER, NOW)).thenReturn(Optional.of(NOW.minusSeconds(600)));

        service.record(USER, "UTC");

        verify(repository).addUsage(USER, LocalDate.parse("2026-11-24"), 0, true, NOW);
    }

    @Test
    void theTimeGoesToTheUsersLocalDay() {
        when(repository.touchLastSeen(USER, NOW)).thenReturn(Optional.of(NOW.minusSeconds(30)));

        // 03:30 UTC es todavia 23 de noviembre en Lima (UTC-5).
        service.record(USER, "America/Lima");

        verify(repository).addUsage(eq(USER), eq(LocalDate.parse("2026-11-23")), eq(30), eq(false), any());
    }

    @Test
    void anInvalidZoneFallsBackToUtc() {
        when(repository.touchLastSeen(USER, NOW)).thenReturn(Optional.empty());

        service.record(USER, "Not/AZone");

        verify(repository).addUsage(USER, LocalDate.parse("2026-11-24"), 0, true, NOW);
    }
}
