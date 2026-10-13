package com.dotani.shared.persistence;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import java.text.Normalizer;
import java.util.Locale;

/**
 * "Contains" matching for Vietnamese text typed with or without diacritics.
 * The column collation already ignores accents ("tinh" = "tình"), but "đ" is a letter of its own,
 * so both sides map đ to d before comparing. LIKE wildcards typed by the user are matched literally.
 */
public final class TextSearch {

    private static final char ESCAPE = '!';

    private TextSearch() {
    }

    public static Predicate contains(CriteriaBuilder criteriaBuilder, Expression<String> field, String text) {
        String needle = text.trim().toLowerCase(Locale.ROOT).replace('đ', 'd');
        String pattern = "%" + needle
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_") + "%";
        Expression<String> normalized = criteriaBuilder.function(
                "replace", String.class, criteriaBuilder.lower(field), criteriaBuilder.literal("đ"), criteriaBuilder.literal("d"));
        return criteriaBuilder.like(normalized, pattern, ESCAPE);
    }

    /** In-memory form of {@link #contains} for lists already loaded: accents and case are ignored. */
    public static boolean matches(String value, String text) {
        if (text == null || text.isBlank()) {
            return true;
        }
        return value != null && fold(value).contains(fold(text.trim()));
    }

    private static String fold(String value) {
        String lower = value.toLowerCase(Locale.ROOT).replace('đ', 'd');
        return Normalizer.normalize(lower, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }
}
