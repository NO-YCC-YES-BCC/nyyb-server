package com.nyyb.nyybserver.ingredient.controller;

import com.nyyb.nyybserver.common.response.GlobalResponse;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientMatchResponseDto;
import com.nyyb.nyybserver.ingredient.service.IngredientService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/ingredients")
@Tag(name = "Ingredient", description = "Ingredient and allergic matching APIs")
public class IngredientController {

    private final IngredientService ingredientService;

    // 복수 productId에 해당하는 성분 + 매칭된 알레르기 물질 조회
    @GetMapping
    public GlobalResponse<IngredientMatchResponseDto> match(@RequestParam List<Long> productIds) {
        return GlobalResponse.ok(ingredientService.match(productIds));
    }
}
