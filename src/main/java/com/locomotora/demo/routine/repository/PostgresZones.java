package com.locomotora.demo.routine.repository;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.zone.ZoneRules;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Nombre de zona horaria que entiende Postgres para {@code AT TIME ZONE}.
 *
 * <p>El telefono (o el navegador) puede mandar un alias como {@code America/Buenos_Aires}, que Java acepta
 * pero el tzdata de Postgres no siempre trae. Si Postgres no conoce el nombre, se usa el primer nombre que
 * si conoce con exactamente las mismas reglas (el canonico, {@code America/Argentina/Buenos_Aires}); si no
 * hay ninguno, UTC. Se calcula una vez por zona.
 */
final class PostgresZones {
    private static final Map<String, String> RESOLVED = new ConcurrentHashMap<>();
    private static volatile Set<String> known;

    private PostgresZones() {
    }

    static String resolve(JdbcTemplate jdbcTemplate, String zoneId) {
        return RESOLVED.computeIfAbsent(zoneId, id -> resolve(id, () -> knownNames(jdbcTemplate)));
    }

    /** Separado para testear sin base. */
    static String resolve(String zoneId, Supplier<Set<String>> postgresNames) {
        Set<String> names = postgresNames.get();
        if (names.contains(zoneId)) {
            return zoneId;
        }
        ZoneRules rules;
        try {
            rules = ZoneId.of(zoneId).getRules();
        } catch (DateTimeException exception) {
            return "UTC";
        }
        return names.stream()
                .filter(name -> name.contains("/"))
                .sorted()
                .filter(name -> sameRules(name, rules))
                .findFirst()
                .orElse("UTC");
    }

    private static boolean sameRules(String name, ZoneRules rules) {
        try {
            return ZoneId.of(name).getRules().equals(rules);
        } catch (DateTimeException exception) {
            return false;
        }
    }

    private static Set<String> knownNames(JdbcTemplate jdbcTemplate) {
        Set<String> names = known;
        if (names == null) {
            names = Set.copyOf(jdbcTemplate.queryForList("SELECT name FROM pg_timezone_names", String.class));
            known = names;
        }
        return names;
    }
}
