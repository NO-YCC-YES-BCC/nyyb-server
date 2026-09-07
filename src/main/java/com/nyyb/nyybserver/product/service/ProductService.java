package com.nyyb.nyybserver.product.service;

import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.service.IngredientIndex;
import com.nyyb.nyybserver.product.data.dto.response.LlmProductIngredientsDto;
import com.nyyb.nyybserver.product.data.dto.response.ProductIngredientMappingDto;
import com.nyyb.nyybserver.product.data.dto.response.ProductSearchDto;
import com.nyyb.nyybserver.product.data.entity.Product;
import com.nyyb.nyybserver.product.data.entity.ProductIngredient;
import com.nyyb.nyybserver.product.data.exception.ProductNotFoundException;
import com.nyyb.nyybserver.product.data.repository.ProductIngredientRepository;
import com.nyyb.nyybserver.product.data.repository.ProductRepository;
import com.nyyb.nyybserver.product.data.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 제품 마스터 이름 검색.
 * 검색어를 공백으로 쪼개 모든 토큰이 품목명에 들어 있는 제품을 찾는다. ("sott 크림" → sott*크림)
 * 조건 구성은 {@link ProductSpecifications} 참고.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int MAX_TOKENS = 5;

    private final ProductRepository productRepository;
    private final ProductIngredientRepository productIngredientRepository;
    private final ProductIngredientFinder productIngredientFinder;
    private final IngredientIndex ingredientIndex;

    @Transactional(readOnly = true)
    public List<ProductSearchDto> search(String keyword, Pageable pageable) {
        List<String> tokens = tokenize(keyword);
        if (tokens.isEmpty()) {
            return List.of();
        }

        Page<Product> found = productRepository.findAll(
                ProductSpecifications.nameStartsWithFirstToken(tokens), pageable);

        // 순서 뒤집힌 검색
        if (found.getTotalElements() == 0) {
            found = productRepository.findAll(
                    ProductSpecifications.nameContainsAllTokens(tokens), pageable);
        }

        return found.getContent().stream()
                .map(ProductSearchDto::from)
                .toList();
    }


    /**
     * 품목명을 LLM에 넘겨 웹에서 전성분을 찾아오게 하고, 받은 성분 표기를 성분 마스터와 매칭해 저장한다.
     * 매칭은 {@link IngredientIndex}가 성분 대표명과 이명(ingredient_alias)을 같은 인덱스에 올려두므로
     * "Ascorbic Acid" 같은 이명 표기도 대표 성분으로 걸린다. 마스터에 없으면 저장하지 않고 unmatched로 돌려준다.
     * 이미 매핑된 전성분이 있으면 LLM을 호출하지 않고 저장돼 있던 매핑을 그대로 돌려준다.
     *
     * 호출자(분석 등)의 트랜잭션과 분리해 독립 트랜잭션으로 돈다.
     * 전성분 조회는 외부 API라 실패가 잦은데, 같은 트랜잭션에 참여하면 여기서 난 예외를
     * 호출자가 잡고 넘어가도 트랜잭션이 rollback-only로 남아 커밋 시점에 전체가 롤백된다.
     *
     * @throws ProductNotFoundException 해당 id의 제품이 없는 경우
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ProductIngredientMappingDto mapIngredientsFromWeb(Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(ProductNotFoundException::new);

        // 이미 매핑된 전성분이 있으면 다시 조회하지 않는다.
        // 웹서치 호출 1건이 1.7만 토큰 규모라 재호출 비용이 크고, 전성분은 자주 바뀌지 않는다.
        List<ProductIngredient> existing = productIngredientRepository.findByProductIdWithIngredient(productId);
        if (!existing.isEmpty()) {
            log.info("이미 매핑된 전성분이 있어 조회를 건너뜁니다. productId={}, count={}", productId, existing.size());
            return alreadyMapped(product, existing);
        }

        String itemName = product.getItemName();
        if (!StringUtils.hasText(itemName)) {
            log.warn("품목명이 없어 전성분을 조회하지 않습니다. productId={}", productId);
            return emptyMapping(productId, itemName);
        }

        LlmProductIngredientsDto found = productIngredientFinder.find(itemName);
        if (!Boolean.TRUE.equals(found.found()) || found.ingredients() == null || found.ingredients().isEmpty()) {
            log.info("전성분 근거를 찾지 못했습니다. productId={}, itemName={}", productId, itemName);
            return emptyMapping(productId, itemName);
        }

        // 같은 성분이 이명 표기로 두 번 올 수 있어 성분 id로 합치고, 표시 순서는 유지한다.
        Map<Long, ProductIngredient> matched = new LinkedHashMap<>();
        Map<Long, String> rawNames = new LinkedHashMap<>();
        List<String> unmatched = new ArrayList<>();

        for (String raw : found.ingredients()) {
            String rawName = normalizeRawName(raw);
            if (rawName.isEmpty()) {
                continue;
            }

            Ingredient ingredient = ingredientIndex.match(rawName);
            if (ingredient == null) {
                unmatched.add(rawName);
                continue;
            }

            matched.putIfAbsent(ingredient.getId(), ProductIngredient.builder()
                    .product(product)
                    .ingredient(ingredient)
                    .rawName(rawName)
                    .build());
            rawNames.putIfAbsent(ingredient.getId(), rawName);
        }

        productIngredientRepository.saveAll(matched.values());

        List<ProductIngredientMappingDto.MatchedIngredient> matchedIngredients = matched.values().stream()
                .map(pi -> new ProductIngredientMappingDto.MatchedIngredient(
                        pi.getIngredient().getId(),
                        pi.getIngredient().getName(),
                        rawNames.get(pi.getIngredient().getId())))
                .toList();

        return new ProductIngredientMappingDto(
                productId, itemName, true, false, found.source(),
                found.ingredients().size(), matchedIngredients.size(), matchedIngredients, unmatched);
    }

    // 함량 괄호와 꼬리 기호를 떼어 성분명만 남긴다. (공백·대소문자는 인덱스가 정규화한다)
    private String normalizeRawName(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replaceAll("[(（].*?[)）]", "")
                .replaceAll("[,·|]+$", "")
                .strip();
    }

    private ProductIngredientMappingDto emptyMapping(Long productId, String itemName) {
        return new ProductIngredientMappingDto(
                productId, itemName, false, false, "", 0, 0, List.of(), List.of());
    }

    // 이미 저장돼 있던 매핑을 그대로 돌려준다. (LLM 호출 없음)
    private ProductIngredientMappingDto alreadyMapped(Product product, List<ProductIngredient> existing) {
        List<ProductIngredientMappingDto.MatchedIngredient> matched = existing.stream()
                .filter(pi -> pi.getIngredient() != null)
                .map(pi -> new ProductIngredientMappingDto.MatchedIngredient(
                        pi.getIngredient().getId(),
                        pi.getIngredient().getName(),
                        pi.getRawName()))
                .toList();

        return new ProductIngredientMappingDto(
                product.getId(), product.getItemName(), true, true, "",
                existing.size(), matched.size(), matched, List.of());
    }

    private List<String> tokenize(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return Arrays.stream(keyword.strip().split("\\s+"))
                .filter(token -> !token.isEmpty())
                .limit(MAX_TOKENS)
                .toList();
    }
}
