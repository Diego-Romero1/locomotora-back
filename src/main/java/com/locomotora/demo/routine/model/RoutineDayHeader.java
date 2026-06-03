package com.locomotora.demo.routine.model;

import java.util.UUID;

public record RoutineDayHeader(UUID id, int dayIndex, String title, String focus) {
}
