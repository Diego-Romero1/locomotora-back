package com.locomotora.demo.ai.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Quita datos personales de los textos antes de enviarlos al proveedor de IA: emails, telefonos y
 * numeros largos (documentos, tarjetas). Lo guardado en la base no cambia; solo lo que sale.
 */
public final class PersonalDataFilter {
    private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
    /** +54 11 5555-1234, 011-5555-1234, (11) 5555.1234: con prefijo + o con guiones/puntos/parentesis. */
    private static final Pattern PHONE = Pattern.compile(
            "(?<![\\w])(\\+\\d[\\d\\s().-]{6,}\\d|\\(?\\d{2,4}\\)?[.-]\\d{3,4}[.-]\\d{3,4})(?![\\w])");
    /** 8 o mas digitos seguidos (o en grupos de 4 con espacios, como una tarjeta). */
    private static final Pattern LONG_NUMBER = Pattern.compile("(?<![\\w.,])(\\d{8,}|\\d{4}(\\s\\d{4}){2,})(?![\\w.,])");

    private PersonalDataFilter() {
    }

    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String result = EMAIL.matcher(text).replaceAll("[email]");
        result = replace(PHONE, result, "[telefono]");
        result = replace(LONG_NUMBER, result, "[numero]");
        return result;
    }

    private static String replace(Pattern pattern, String text, String replacement) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            long digits = matcher.group().chars().filter(Character::isDigit).count();
            matcher.appendReplacement(out, digits >= 8 ? Matcher.quoteReplacement(replacement) : Matcher.quoteReplacement(matcher.group()));
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
