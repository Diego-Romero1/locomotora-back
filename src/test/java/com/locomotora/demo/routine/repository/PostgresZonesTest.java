package com.locomotora.demo.routine.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Set;
import org.junit.jupiter.api.Test;

class PostgresZonesTest {
    private final Set<String> postgres = Set.of("UTC", "America/Argentina/Buenos_Aires", "America/Argentina/Cordoba", "Europe/Madrid");

    @Test
    void aNameThatPostgresKnowsIsUsedAsIs() {
        assertEquals("Europe/Madrid", PostgresZones.resolve("Europe/Madrid", () -> postgres));
    }

    @Test
    void anAliasMapsToTheZoneWithTheSameRules() {
        // Chrome y algunos telefonos informan el alias viejo.
        assertEquals("America/Argentina/Buenos_Aires", PostgresZones.resolve("America/Buenos_Aires", () -> postgres));
    }

    @Test
    void withoutAMatchItFallsBackToUtc() {
        assertEquals("UTC", PostgresZones.resolve("Asia/Tokyo", () -> postgres));
        assertEquals("UTC", PostgresZones.resolve("Mars/Olympus", () -> postgres));
    }
}
