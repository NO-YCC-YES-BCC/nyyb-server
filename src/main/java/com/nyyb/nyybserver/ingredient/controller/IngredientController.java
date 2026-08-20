package com.nyyb.nyybserver.ingredient.controller;

import com.nyyb.nyybserver.common.response.GlobalResponse;
import com.nyyb.nyybserver.common.dto.PageRequestDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientSummaryDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.ProductIngredientMatchDto;
import com.nyyb.nyybserver.ingredient.service.IngredientService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ingredients")
@Tag(name = "Ingredient", description = "Ingredient and allergic matching APIs")
public class IngredientController {

    private final IngredientService ingredientService;

    // 복수 productId에 해당하는 성분 + 매칭된 알레르기 물질을 제품별로 묶어 조회
    @Operation
    @GetMapping("/match")
    public GlobalResponse<List<ProductIngredientMatchDto>> match(@RequestParam List<Long> productIds) {
        return GlobalResponse.ok(ingredientService.match(productIds));
    }

    @Operation
    @GetMapping
    public GlobalResponse<List<IngredientSummaryDto>> getIngredients(@ParameterObject PageRequestDto pageRequest) {
        return GlobalResponse.ok(ingredientService.getIngredients(pageRequest.toPageable()));
    }

    @Operation
    @GetMapping("/{ingredientId}")
    public GlobalResponse<IngredientDto> getIngredient(@PathVariable Long ingredientId) {
        return GlobalResponse.ok(ingredientService.getIngredient(ingredientId));
    }
}
