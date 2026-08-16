package com.nyyb.nyybserver.analysis.service;

import com.nyyb.nyybserver.analysis.data.dto.response.OcrIngredientDto;
import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductRepository;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.service.IngredientIndex;
import com.nyyb.nyybserver.user.data.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductWriter {

    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final IngredientIndex ingredientIndex;
    private final UserRepository userRepository;

    // 저장 결과
    // 응답 조립에 필요한 값은 트랜잭션 안에서 미리 담아 반환
    public record Result(Long productId, int ingredientCount, List<OcrIngredientDto> ingredients) {
    }

    // 매칭된 성분 제품에 매핑. userId를 소유자로 지정해 이후 analyze 단계에서 소유권 검증이 가능하게 한다.
    @Transactional
    public Result save(Long userId, String imageKey, ProductCategory category, String ocrText) {
        Product product = Product.builder()
                .user(userRepository.getReferenceById(userId))
                .imageKey(imageKey)
                .category(category)
                .ocrText(ocrText)
                .build();
        productRepository.save(product);

        List<OcrIngredientDto> ingredients = matchIngredients(ocrText, product);

        // 매칭된 성분 갯수를 제품에 반영(더티 체킹)
        product.updateIngredientCount(ingredients.size());

        return new Result(product.getId(), ingredients.size(), ingredients);
    }

    // ocrText -> 성분 인덱스 조회 -> ProductIngredient 저장 후 요약 반환
    private List<OcrIngredientDto> matchIngredients(String ocrText, Product product) {
        if (!StringUtils.hasText(ocrText)) {
            return List.of();
        }

        // 성분 id 기준 중복 제거 + 입력 순서 유지
        Map<Long, ProductIngredient> matched = new LinkedHashMap<>();

        // 괄호(함량 표기 등) -> ()분리
        String normalizedText = ocrText.replaceAll("[(（]", ", (");

        // 쉼표/줄바꿈으로 분리
        // but , 앞뒤 숫자인경우는 분리 X
        for (String token : normalizedText.split("(?<!\\d),|,(?!\\d)|\\n")) {

            String rawName = token.strip().replaceAll("[\\s|]+$", "");
            if (rawName.isEmpty()) {
                continue;
            }

            Ingredient ingredient = ingredientIndex.match(rawName);
            if (ingredient == null) {
                continue; // 매칭 실패 -> 저장 X
            }

            matched.putIfAbsent(ingredient.getId(), ProductIngredient.builder()
                    .product(product)
                    .ingredient(ingredient)
                    .rawName(rawName)
                    .build());
        }

        productIngredientRepository.saveAll(matched.values());

        return matched.values().stream()
                .map(pi -> toOcrIngredient(pi.getIngredient()))
                .toList();
    }

    private OcrIngredientDto toOcrIngredient(Ingredient ingredient) {
        return OcrIngredientDto.builder()
                .ingredientId(ingredient.getId())
                .name(ingredient.getName())
                .isToxic(ingredient.getIsToxic())
                .riskLevel(ingredient.getRiskLevel())
                .build();
    }
}
