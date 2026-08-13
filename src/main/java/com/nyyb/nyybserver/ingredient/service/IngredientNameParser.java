package com.nyyb.nyybserver.ingredient.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component
public class IngredientNameParser {

    public List<String> parse(String rawText) {
        if (!StringUtils.hasText(rawText)) {
            return List.of();
        }

        List<String> ingredients = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < rawText.length(); i++) {
            char character = rawText.charAt(i);
            if (isDelimiter(rawText, i, character)) {
                addIfNotBlank(ingredients, current);
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        addIfNotBlank(ingredients, current);

        return ingredients;
    }

    private boolean isDelimiter(String text, int index, char character) {
        if (character == '\n' || character == '\r' || character == ';' || character == '；') {
            return true;
        }
        if (character == ',' || character == '，' || character == '、') {
            return !isCommaBetweenDigits(text, index);
        }
        return false;
    }

    private boolean isCommaBetweenDigits(String text, int index) {
        int previous = previousNonWhitespaceIndex(text, index - 1);
        int next = nextNonWhitespaceIndex(text, index + 1);
        return previous >= 0
                && next >= 0
                && Character.isDigit(text.charAt(previous))
                && Character.isDigit(text.charAt(next));
    }

    private int previousNonWhitespaceIndex(String text, int index) {
        for (int i = index; i >= 0; i--) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private int nextNonWhitespaceIndex(String text, int index) {
        for (int i = index; i < text.length(); i++) {
            if (!Character.isWhitespace(text.charAt(i))) {
                return i;
            }
        }
        return -1;
    }

    private void addIfNotBlank(List<String> ingredients, StringBuilder current) {
        String ingredient = current.toString().trim();
        if (StringUtils.hasText(ingredient)) {
            ingredients.add(ingredient);
        }
    }
}
