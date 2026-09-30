package com.heaterworkshop.domain.inventory;

import java.text.Normalizer;
import java.util.Locale;

public final class CatalogText {
    private CatalogText() {}
    public static String required(String value, int maximum) {
        if (value == null) throw new IllegalArgumentException("El campo es obligatorio.");
        String text = Normalizer.normalize(value, Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ").strip();
        if (text.isBlank() || text.length() > maximum || text.codePoints().anyMatch(Character::isISOControl))
            throw new IllegalArgumentException("Texto vacío, demasiado largo o con caracteres no permitidos.");
        return text;
    }
    public static String key(String value) { return value.toLowerCase(Locale.ROOT); }
}
