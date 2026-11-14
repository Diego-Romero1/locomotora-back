package com.locomotora.demo.ai.service;

import java.util.Locale;

/**
 * Respuestas por reglas cuando Claude no esta disponible (sin clave, sin red o limite diario).
 * Son genericas a proposito: solo usan datos del usuario que ya estan guardados.
 */
final class LocalCoach {
    static final String MODEL = "local-context-coach-v1";

    private LocalCoach() {
    }

    static String answer(UserContext context, String message) {
        String goal = context.goals().isEmpty() ? "mantenerte en forma" : context.goals().get(0);
        String routine = context.activeRoutines().isEmpty() ? "tu rutina actual" : context.activeRoutines().get(0);
        int sessions = context.sessionsLast28Days();
        String lower = message.toLowerCase(Locale.ROOT);

        if (lower.contains("nutri") || lower.contains("comida") || lower.contains("prote") || lower.contains("dieta")) {
            String protein = context.weightKg() == null
                    ? "entre 1,6 y 2,2 g de proteina por kg de peso al dia"
                    : "entre " + Math.round(context.weightKg() * 1.6) + " y " + Math.round(context.weightKg() * 2.2)
                            + " g de proteina al dia (1,6 a 2,2 g por kg)";
            return "Para tu objetivo (" + goal + "), apunta a " + protein
                    + ", repartida en 3 o 4 comidas. Pon hidratos alrededor del entrenamiento y bebe agua durante el dia. "
                    + "Si tienes una condicion medica, consulta con un profesional antes de cambiar tu dieta.";
        }
        if (lower.contains("rutina") || lower.contains("entren") || lower.contains("ejercicio")) {
            return "Tu foco actual es " + goal + ". Usa " + routine + " como base: en los ultimos 28 dias completaste "
                    + sessions + " sesiones. Para progresar, cuando completes todas las series en el tope de repeticiones, "
                    + "sube un poco el peso la siguiente vez. Si una semana se siente demasiado pesada, baja el volumen antes de cambiar de plan.";
        }
        if (lower.contains("peso") || lower.contains("metrica") || lower.contains("progreso")) {
            return "Mira la tendencia, no una medicion aislada. Registra tu peso 1 o 2 veces por semana, a la misma hora, "
                    + "y comparalo con tu constancia: llevas " + sessions + " sesiones en los ultimos 28 dias.";
        }
        return "Puedo ayudarte con tus rutinas, la tecnica de cada ejercicio, nutricion y progreso usando tus datos. "
                + "Cuentame que quieres mejorar y cuantos dias puedes entrenar.";
    }
}
