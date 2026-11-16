package com.locomotora.demo.ai.service;

import com.locomotora.demo.ai.tools.ExerciseCatalogFilter;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Controles de seguridad que no dependen del modelo:
 * <ul>
 *   <li>Antes: detecta senales de riesgo en la pregunta y agrega instrucciones al prompt.</li>
 *   <li>Despues: agrega siempre un aviso fijo cuando hubo una senal de riesgo, y revisa la
 *       respuesta por recomendaciones peligrosas (calorias muy bajas, ayunos largos).</li>
 * </ul>
 */
public final class SafetyGuard {
    public enum Risk { CARDIO, EATING_DISORDER, PREGNANCY, INJURY, SUBSTANCES, MINOR }

    public enum Issue { VERY_LOW_CALORIES, LONG_FAST }

    private static final Map<Risk, Pattern> RISKS = Map.of(
            Risk.CARDIO, Pattern.compile("dolor (en el|de) pecho|presion en el pecho|opresion en el pecho|palpitaciones|me desmay|desmayo|"
                    + "mareo|falta de aire|no puedo respirar|me falta el aire|chest pain|faint|dizz|short(ness)? of breath"),
            Risk.EATING_DISORDER, Pattern.compile("anorexi|bulimi|atracon|vomit|purgar|dejar de comer|no comer nada|laxante|binge|purg"),
            Risk.PREGNANCY, Pattern.compile("embaraz|gestacion|lactancia|pregnan|breastfeed"),
            Risk.INJURY, Pattern.compile("lesion|desgarr|fractur|esguince|hernia|tendinitis|me duele mucho|dolor agudo|dolor fuerte|"
                    + "me operaron|cirugia|injur|\\btorn\\b|sprain|sharp pain"),
            Risk.SUBSTANCES, Pattern.compile("esteroid|anabolic|anabolizant|testosteron|clembuterol|trembolona|\\bsarms?\\b|steroid"),
            Risk.MINOR, Pattern.compile("tengo 1[0-5] anos|soy menor|i'?m 1[0-5]\\b")
    );

    private static final Map<Risk, String> INSTRUCTIONS = Map.of(
            Risk.CARDIO, "El usuario menciona sintomas cardiovasculares o respiratorios: indica parar el ejercicio y consultar a un medico; "
                    + "si son intensos o en reposo, buscar atencion de urgencia. No des un plan de entrenamiento para eso.",
            Risk.EATING_DISORDER, "El usuario menciona conductas de trastorno alimentario: responde con empatia, no des cifras de calorias "
                    + "ni pautas de restriccion, y sugiere hablar con un profesional de salud.",
            Risk.PREGNANCY, "El usuario menciona embarazo o lactancia: da solo pautas generales y prudentes, y pide validarlas con su medico.",
            Risk.INJURY, "El usuario menciona una lesion o dolor: no diagnostiques; sugiere evaluacion profesional y, si propones algo, "
                    + "que sean alternativas que no carguen la zona afectada.",
            Risk.SUBSTANCES, "El usuario pregunta por sustancias para mejorar el rendimiento: no des dosis ni protocolos; explica riesgos "
                    + "y recomienda consultar a un medico.",
            Risk.MINOR, "El usuario podria ser menor de edad: recomienda supervision de un adulto o entrenador y evita dietas o cargas maximas."
    );

    private static final Map<Risk, String> NOTICES = Map.of(
            Risk.CARDIO, "Si tienes dolor en el pecho, mareos o falta de aire, deja de entrenar y consulta a un médico. Si es intenso, busca atención de urgencia.",
            Risk.EATING_DISORDER, "Si sientes que tu relación con la comida te hace daño, habla con un profesional de salud. No estás solo.",
            Risk.PREGNANCY, "En el embarazo o la lactancia, valida cualquier cambio de entrenamiento o alimentación con tu médico.",
            Risk.INJURY, "Ante dolor o una lesión, pide una evaluación a un médico o fisioterapeuta antes de cargar la zona.",
            Risk.SUBSTANCES, "Las sustancias para mejorar el rendimiento tienen riesgos serios para la salud. Consúltalo con un médico.",
            Risk.MINOR, "Si eres menor de edad, entrena con la supervisión de un adulto o un entrenador."
    );

    private static final Pattern CALORIES = Pattern.compile("(\\d{3,4})\\s*(kcal|calorias|cal\\b)");
    private static final Pattern FAST = Pattern.compile("ayun\\w*\\s+(de\\s+)?(\\d{2,3})\\s*(h\\b|horas)|ayun\\w*\\s+(de\\s+)?(\\d+|varios|dos|tres)\\s+dias");

    private SafetyGuard() {
    }

    public static Set<Risk> detect(String text) {
        String normalized = ExerciseCatalogFilter.normalize(text);
        Set<Risk> risks = EnumSet.noneOf(Risk.class);
        RISKS.forEach((risk, pattern) -> {
            if (pattern.matcher(normalized).find()) {
                risks.add(risk);
            }
        });
        return risks;
    }

    /** Instrucciones extra para el prompt de sistema. */
    public static String instructions(Set<Risk> risks) {
        if (risks.isEmpty()) {
            return "";
        }
        StringBuilder text = new StringBuilder("\n\nAtencion en este mensaje:");
        for (Risk risk : Risk.values()) {
            if (risks.contains(risk)) {
                text.append("\n- ").append(INSTRUCTIONS.get(risk));
            }
        }
        return text.toString();
    }

    public static Set<Issue> review(String answer) {
        String normalized = ExerciseCatalogFilter.normalize(answer);
        Set<Issue> issues = EnumSet.noneOf(Issue.class);
        Matcher calories = CALORIES.matcher(normalized);
        while (calories.find()) {
            int value = Integer.parseInt(calories.group(1));
            if (value >= 300 && value < 1200 && !normalized.contains("quemas") && !normalized.contains("gastas")) {
                issues.add(Issue.VERY_LOW_CALORIES);
            }
        }
        Matcher fast = FAST.matcher(normalized);
        while (fast.find()) {
            String hours = fast.group(2);
            if (hours == null || Integer.parseInt(hours) >= 24) {
                issues.add(Issue.LONG_FAST);
            }
        }
        return issues;
    }

    /** Respuesta final: la del modelo mas los avisos fijos que correspondan. */
    public static String withNotices(String answer, Set<Risk> risks, Set<Issue> issues) {
        StringBuilder text = new StringBuilder(answer);
        List<String> notices = new java.util.ArrayList<>();
        for (Risk risk : Risk.values()) {
            if (risks.contains(risk)) {
                notices.add(NOTICES.get(risk));
            }
        }
        if (issues.contains(Issue.VERY_LOW_CALORIES)) {
            notices.add("Comer menos de 1200 kcal al día no es recomendable sin supervisión de un profesional.");
        }
        if (issues.contains(Issue.LONG_FAST)) {
            notices.add("Los ayunos de 24 horas o más pueden ser riesgosos: consúltalo con un profesional de salud.");
        }
        if (!notices.isEmpty()) {
            text.append("\n\n⚠️ ").append(String.join(" ", notices));
        }
        return text.toString();
    }
}
