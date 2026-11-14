package com.locomotora.demo.ai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Lo que el usuario pide al generar una rutina. Todo es opcional: lo que falte se toma del perfil.
 *
 * @param prompt       peticion libre ("quiero enfocarme en gluteo", "tengo dolor de hombro").
 * @param focusMuscles grupos a priorizar, con los nombres del catalogo.
 * @param model        modelo para esta generacion; si falta, el elegido en Perfil o el por defecto.
 */
public record RoutineGenerationRequest(
        @Size(max = 1000) String prompt,
        @Min(1) @Max(7) Integer daysPerWeek,
        @Min(15) @Max(180) Integer sessionMinutes,
        String objective,
        String level,
        @Size(max = 10) List<String> focusMuscles,
        String model
) {
}
