package com.nyyb.nyybserver.ingredient.service;

import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import com.nyyb.nyybserver.analysis.data.exception.ProductNotFoundException;
import com.nyyb.nyybserver.analysis.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.analysis.data.repository.ProductRepository;
import com.nyyb.nyybserver.ingredient.data.dto.response.AllergicDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientSummaryDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.ProductIngredientMatchDto;
import com.nyyb.nyybserver.ingredient.data.entity.Allergic;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.repository.AllergicRepository;
import com.nyyb.nyybserver.ingredient.data.exception.IngredientNotFoundException;
import com.nyyb.nyybserver.ingredient.data.repository.IngredientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngredientService {

    private final ProductIngredientRepository productIngredientRepository;
    private final ProductRepository productRepository;
    private final AllergicRepository allergicRepository;
    private final IngredientRepository ingredientRepository;

    /**
     * 전체 성분을 이름순으로 페이징 조회한다. (정렬 조건은 받지 않고 이름 → id 순으로 고정)
     *
     * @param pageable page/size 페이징 정보
     * @return 성분 목록
     */
    @Transactional(readOnly = true)
    public List<IngredientSummaryDto> getIngredients(Pageable pageable) {
        return ingredientRepository.findAllByOrderByNameAscIdAsc(pageable).stream()
                .map(IngredientSummaryDto::from)
                .toList();
    }

    /**
     * 성분 id로 성분 상세를 조회한다.
     *
     * @param ingredientId 조회할 성분 id
     * @return 성분 상세
     * @throws IngredientNotFoundException 해당 id의 성분이 없는 경우
     */
    @Transactional(readOnly = true)
    public IngredientDto getIngredient(Long ingredientId) {
        Ingredient ingredient = ingredientRepository.findById(ingredientId)
                .orElseThrow(IngredientNotFoundException::new);
        return IngredientDto.from(ingredient);
    }

    /**
     * 단일 제품의 성분/알레르기 매칭 결과를 조회한다.
     *
     * @param productId 조회할 제품 id
     * @return 해당 제품의 매칭 결과
     * @throws ProductNotFoundException 해당 id의 제품이 없는 경우
     */
    @Transactional(readOnly = true)
    public ProductIngredientMatchDto match(Long productId) {
        return match(List.of(productId)).getFirst();
    }

    /**
     * 복수 제품의 성분을 제품별로 묶어, RiskLevel이 null인 성분은 제외하고 남은 성분을 알레르기 유발 물질 DB와 매칭한다.
     * 한 제품 안의 중복 성분은 성분 id 기준으로 한 번만 반환하며, 매칭된 알레르기 물질도 제품별로 중복 없이 반환한다.
     *
     * @param productIds 조회할 제품 id 목록 (중복은 무시하고 요청 순서를 유지)
     * @return 제품별 매칭 결과 (제품 id/이름 + RiskLevel이 존재하는 성분 + 매칭된 알레르기 물질)
     * @throws ProductNotFoundException 목록에 존재하지 않는 제품 id가 있는 경우
     */
    @Transactional(readOnly = true)
    public List<ProductIngredientMatchDto> match(List<Long> productIds) {
        List<Long> distinctProductIds = productIds.stream().distinct().toList();

        // 1. 알레르기 물질을 정규화한 이름 -> Allergic 맵으로 로드 (OCR 성분 매칭과 동일한 정규화 사용)
        Map<String, Allergic> allergicIndex = new LinkedHashMap<>();
        for (Allergic allergic : allergicRepository.findAll()) {
            allergicIndex.putIfAbsent(normalize(allergic.getName()), allergic);
        }

        // 2. 제품/제품 성분을 한 번에 조회해 제품 id 기준으로 묶는다
        Map<Long, Product> productsById = productRepository.findAllById(distinctProductIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, List<ProductIngredient>> productIngredientsByProductId =
                productIngredientRepository.findByProductIdInWithIngredient(distinctProductIds).stream()
                        .collect(Collectors.groupingBy(productIngredient -> productIngredient.getProduct().getId()));

        List<ProductIngredientMatchDto> matches = new ArrayList<>();
        for (Long productId : distinctProductIds) {
            Product product = productsById.get(productId);
            if (product == null) {
                throw new ProductNotFoundException();
            }
            matches.add(toMatch(product,
                    productIngredientsByProductId.getOrDefault(productId, List.of()),
                    allergicIndex));
        }

        log.info("성분 매칭 완료: productIds={}, 제품 {}개", distinctProductIds, matches.size());
        return matches;
    }

    /** 한 제품의 성분에서 RiskLevel이 null인 성분을 제외하고, 남은 성분을 알레르기 물질과 매칭한다. */
    private ProductIngredientMatchDto toMatch(
            Product product,
            List<ProductIngredient> productIngredients,
            Map<String, Allergic> allergicIndex
    ) {
        // RiskLevel이 null인 성분 제외 + 성분 id 기준 중복 제거
        Map<Long, Ingredient> ingredientsById = new LinkedHashMap<>();
        for (ProductIngredient productIngredient : productIngredients) {
            Ingredient ingredient = productIngredient.getIngredient();
            // 마스터 매칭 실패(null) 또는 RiskLevel이 null인 성분은 제외
            if (ingredient == null || ingredient.getRiskLevel() == null) {
                continue;
            }
            ingredientsById.putIfAbsent(ingredient.getId(), ingredient);
        }

        // 남은 성분을 알레르기 물질과 매칭 (Allergic id 기준 중복 제거)
        Map<Long, Allergic> matchedAllergicsById = new LinkedHashMap<>();
        for (Ingredient ingredient : ingredientsById.values()) {
            Allergic allergic = allergicIndex.get(normalize(ingredient.getName()));
            if (allergic != null) {
                matchedAllergicsById.putIfAbsent(allergic.getId(), allergic);
            }
        }

        return new ProductIngredientMatchDto(
                product.getId(),
                product.getProductName(),
                ingredientsById.values().stream().map(IngredientDto::from).toList(),
                matchedAllergicsById.values().stream().map(AllergicDto::from).toList()
        );
    }

    // 소문자화 + 모든 공백 제거 (IngredientIndex와 동일한 정규화 규칙)
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase().replaceAll("\\s+", "");
    }
}
