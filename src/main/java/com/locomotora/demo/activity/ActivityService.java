package com.locomotora.demo.activity;

import com.locomotora.demo.common.CurrentUser;
import com.locomotora.demo.common.TimeZones;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tiempo de uso de la app. Mientras esta abierta, la app manda un ping al abrirse, cada minuto y al pasar a
 * segundo plano. El servidor suma el tiempo entre dos pings seguidos si estan a menos de {@link #SESSION_GAP}
 * (la app siguio abierta); si no, empieza una sesion nueva. La app no informa duraciones, asi que no puede
 * inflarlas.
 */
@Service
public class ActivityService {
    static final Duration SESSION_GAP = Duration.ofMinutes(2);

    private final ActivityRepository activityRepository;
    private final Clock clock;

    @Autowired
    public ActivityService(ActivityRepository activityRepository) {
        this(activityRepository, Clock.systemUTC());
    }

    ActivityService(ActivityRepository activityRepository, Clock clock) {
        this.activityRepository = activityRepository;
        this.clock = clock;
    }

    @Transactional
    public void ping(String timeZone) {
        record(CurrentUser.id(), timeZone);
    }

    void record(UUID userId, String timeZone) {
        Instant now = clock.instant();
        Optional<Instant> previous = activityRepository.touchLastSeen(userId, now);
        long gap = previous.map(at -> Duration.between(at, now).getSeconds()).orElse(-1L);
        boolean sameSession = gap >= 0 && gap <= SESSION_GAP.getSeconds();
        activityRepository.addUsage(
                userId,
                now.atZone(TimeZones.parse(timeZone)).toLocalDate(),
                sameSession ? (int) gap : 0,
                !sameSession,
                now
        );
    }
}
