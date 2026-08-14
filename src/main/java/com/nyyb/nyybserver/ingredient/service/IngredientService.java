package com.nyyb.nyybserver.ingredient.service;

import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.ingredient.data.dto.response.AllergicDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientMatchResponseDto;
import com.nyyb.nyybserver.ingredient.data.entity.Allergic;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.repository.AllergicRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientService {

    private final ProductIngredientRepository productIngredientRepository;
    private final AllergicRepository allergicRepository;

    /**
     * 복수 제품의 성분을 모아 RiskLevel이 null인 성분은 제외하고, 남은 성분을 알레르기 유발 물질 DB와 매칭한다.
     * 제품 간 중복 성분은 성분 id 기준으로 한 번만 반환하며, 매칭된 알레르기 물질도 중복 없이 반환한다.
     *
     * @param productIds 조회할 제품 id 목록
     * @return RiskLevel이 존재하는 성분(중복 제거) + 매칭된 알레르기 물질(중복 제거)
     */
    @Transactional(readOnly = true)
    public IngredientMatchResponseDto match(List<Long> productIds) {
        // 1. 알레르기 물질을 정규화한 이름 -> Allergic 맵으로 로드 (OCR 성분 매칭과 동일한 정규화 사용)
        Map<String, Allergic> allergicIndex = new LinkedHashMap<>();
        for (Allergic allergic : allergicRepository.findAll()) {
            allergicIndex.putIfAbsent(normalize(allergic.getName()), allergic);
        }

        // 2. 제품별 성분 수집 → RiskLevel이 null인 성분 제외 + 성분 id 기준 중복 제거
        Map<Long, Ingredient> ingredientsById = new LinkedHashMap<>();
        for (Long productId : productIds) {
            for (ProductIngredient productIngredient :
                    productIngredientRepository.findByProductIdWithIngredient(productId)) {
                Ingredient ingredient = productIngredient.getIngredient();
                // 마스터 매칭 실패(null) 또는 RiskLevel이 null인 성분은 제외
                if (ingredient == null || ingredient.getRiskLevel() == null) {
                    continue;
                }
                ingredientsById.putIfAbsent(ingredient.getId(), ingredient);
            }
        }

        // 3. 남은 성분을 알레르기 물질과 매칭 (Allergic id 기준 중복 제거)
        Map<Long, Allergic> matchedAllergicsById = new LinkedHashMap<>();
        for (Ingredient ingredient : ingredientsById.values()) {
            Allergic allergic = allergicIndex.get(normalize(ingredient.getName()));
            if (allergic != null) {
                matchedAllergicsById.putIfAbsent(allergic.getId(), allergic);
            }
        }

        List<IngredientDto> ingredients = ingredientsById.values().stream()
                .map(IngredientDto::from)
                .toList();
        List<AllergicDto> allergics = matchedAllergicsById.values().stream()
                .map(AllergicDto::from)
                .toList();

        log.info("성분 매칭 완료: productIds={}, 성분 {}개, 알레르기 물질 {}개",
                productIds, ingredients.size(), allergics.size());
        return new IngredientMatchResponseDto(ingredients, allergics);
    }

    // 소문자화 + 모든 공백 제거 (IngredientIndex와 동일한 정규화 규칙)
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase().replaceAll("\\s+", "");
    }
}
