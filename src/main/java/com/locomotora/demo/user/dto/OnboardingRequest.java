package com.locomotora.demo.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Lo que pide el onboarding. Los campos obligatorios son los que el recomendador y la IA
 * necesitan para armar un plan; los vocabularios se validan en {@code OnboardingRules}.
 */
public record OnboardingRequest(
        @NotBlank String goalType,
        @NotBlank String experienceLevel,
        @NotNull @Min(1) @Max(7) Integer trainingDaysPerWeek,
        @NotNull @Min(15) @Max(180) Integer sessionDurationMinutes,
        @NotNull @DecimalMin("120") @DecimalMax("230") BigDecimal heightCm,
        @NotNull @DecimalMin("30") @DecimalMax("250") BigDecimal weightKg,
        String preferredTrainingStyle,
        @NotEmpty @Size(max = 10) List<String> equipment,
        @Size(max = 12) List<@Valid LimitationItem> limitations,
        /* La edad la calcula el sistema; OnboardingRules revisa que de entre 13 y 100 anos. */
        @NotNull @Past LocalDate birthDate,
        String sex,
        String activityLevel,
        String trainingLocation,
        /* Chequeo de salud: lista vacia = nada aplica. Obligatorio: la pregunta debe responderse. */
        @NotNull @Size(max = 10) List<String> healthFlags
) {
    public record LimitationItem(String limitationType, @Size(max = 300) String notes) {
    }
}
