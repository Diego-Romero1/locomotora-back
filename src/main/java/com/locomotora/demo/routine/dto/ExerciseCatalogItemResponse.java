package com.locomotora.demo.routine.dto;

import java.util.List;

/**
 * Ejercicio del catalogo. stressZones: zonas del cuerpo que carga de forma importante (KNEE,
 * LOWER_BACK, ...), para cuidar las lesiones que el usuario declara.
 *
 * <p>GET /exercises devuelve la version liviana (description e instructions en null); el detalle
 * con la tecnica sale de GET /exercises/{id}. muscleGroup es el grupo para navegar el catalogo
 * (Piernas, Espalda, Estiramientos...); primaryMuscleGroup, el musculo principal.
 *
 * <p>mediaFrames: cuadros de la ilustracion (inicio, final...) que la app alterna, como rutas relativas al
 * servidor de media; mediaAuthor, mediaLicense y mediaSourceUrl son la atribucion que hay que mostrar.
 */
public record ExerciseCatalogItemResponse(
        String id,
        String name,
        String category,
        String primaryMuscleGroup,
        String equipment,
        String difficulty,
        String description,
        String instructions,
        boolean custom,
        List<String> stressZones,
        String muscleGroup,
        List<String> secondaryMuscles,
        String nameEn,
        String exerciseType,
        String mechanic,
        boolean core,
        String thumbUrl,
        List<String> mediaFrames,
        String mediaAuthor,
        String mediaLicense,
        String mediaSourceUrl
) {
    public ExerciseCatalogItemResponse {
        stressZones = stressZones == null ? List.of() : List.copyOf(stressZones);
        secondaryMuscles = secondaryMuscles == null ? List.of() : List.copyOf(secondaryMuscles);
        mediaFrames = mediaFrames == null ? List.of() : List.copyOf(mediaFrames);
        exerciseType = exerciseType == null ? "STRENGTH" : exerciseType;
    }

    public ExerciseCatalogItemResponse(String id, String name, String category, String primaryMuscleGroup,
            String equipment, String difficulty, String description, String instructions, boolean custom,
            List<String> stressZones) {
        this(id, name, category, primaryMuscleGroup, equipment, difficulty, description, instructions, custom,
                stressZones, ExerciseGroups.groupOf(primaryMuscleGroup, null), List.of(), null, null, null, true, null,
                List.of(), null, null, null);
    }

    public ExerciseCatalogItemResponse(String id, String name, String category, String primaryMuscleGroup,
            String equipment, String difficulty, String description, String instructions, boolean custom) {
        this(id, name, category, primaryMuscleGroup, equipment, difficulty, description, instructions, custom, List.of());
    }
}
