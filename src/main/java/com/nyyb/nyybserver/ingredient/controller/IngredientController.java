package com.nyyb.nyybserver.ingredient.controller;

import com.nyyb.nyybserver.ingredient.data.dto.request.IngredientMatchRequestDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientMatchResponseDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientMatchResult;
import com.nyyb.nyybserver.ingredient.data.enums.IngredientMatchStatus;
import com.nyyb.nyybserver.ingredient.service.IngredientMatchingService;
import com.nyyb.nyybserver.ingredient.service.IngredientNameParser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ingredients")
@Tag(name = "Ingredients", description = "Ingredient matching APIs")
public class IngredientController {

    private final IngredientMatchingService ingredientMatchingService;
    private final IngredientNameParser ingredientNameParser;

    @Operation(summary = "Match OCR ingredient names")
    @PostMapping("/match")
    public ResponseEntity<IngredientMatchResponseDto> match(
            @RequestBody IngredientMatchRequestDto request
    ) {
        List<String> inputNames = resolveInputNames(request);
        List<IngredientMatchResult> results = ingredientMatchingService.match(inputNames);

        return ResponseEntity.ok(IngredientMatchResponseDto.builder()
                .totalInputCount(countNonBlank(inputNames))
                .resultCount(results.size())
                .matchedCount(countByStatus(results, IngredientMatchStatus.MATCHED))
                .regulatedIngredientCount((int) results.stream()
                        .filter(result -> Boolean.TRUE.equals(result.getRegulated()))
                        .count())
                .notFoundCount(countByStatus(results, IngredientMatchStatus.NOT_FOUND))
                .ambiguousCount(countByStatus(results, IngredientMatchStatus.AMBIGUOUS))
                .notFoundNames(namesByStatus(results, IngredientMatchStatus.NOT_FOUND))
                .ambiguousNames(namesByStatus(results, IngredientMatchStatus.AMBIGUOUS))
                .results(results)
                .build());
    }

    private List<String> resolveInputNames(IngredientMatchRequestDto request) {
        if (request == null) {
            return List.of();
        }
        if (request.getIngredients() != null && !request.getIngredients().isEmpty()) {
            return request.getIngredients();
        }
        return ingredientNameParser.parse(request.getRawText());
    }

    private int countNonBlank(List<String> inputNames) {
        return (int) inputNames.stream()
                .filter(StringUtils::hasText)
                .count();
    }

    private int countByStatus(List<IngredientMatchResult> results, IngredientMatchStatus status) {
        return (int) results.stream()
                .filter(result -> result.getMatchStatus() == status)
                .count();
    }

    private List<String> namesByStatus(List<IngredientMatchResult> results, IngredientMatchStatus status) {
        return results.stream()
                .filter(result -> result.getMatchStatus() == status)
                .map(IngredientMatchResult::getInputName)
                .toList();
    }
}
