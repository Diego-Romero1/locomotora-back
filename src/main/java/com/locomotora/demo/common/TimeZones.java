package com.locomotora.demo.common;

import java.time.DateTimeException;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Zona horaria que manda el telefono ({@code tz}); invalida o vacia = UTC. */
public final class TimeZones {
    private TimeZones() {
    }

    public static ZoneId parse(String timeZone) {
        if (timeZone == null || timeZone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timeZone.trim());
        } catch (DateTimeException exception) {
            return ZoneOffset.UTC;
        }
    }
}
