package com.nyyb.nyybserver.ingredient.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.text.Normalizer;
import java.util.Locale;

@Component
public class IngredientNameNormalizer {

    private static final String WHITESPACE_PATTERN = "\\s+";
    private static final String HYPHEN_VARIANTS = "[‐‑‒–—―−﹘﹣－]";

    public String normalize(String ingredientName) {
        return normalizeForSearch(ingredientName);
    }

    public static String normalizeForSearch(String ingredientName) {
        if (!StringUtils.hasText(ingredientName)) {
            return "";
        }

        return Normalizer.normalize(ingredientName.trim(), Normalizer.Form.NFKC)
                .replaceAll(HYPHEN_VARIANTS, "-")
                .toLowerCase(Locale.ROOT)
                .replaceAll(WHITESPACE_PATTERN, "");
    }
}
