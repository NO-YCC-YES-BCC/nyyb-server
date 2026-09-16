package com.nyyb.nyybserver.product.service;

import com.nyyb.nyybserver.product.data.dto.response.ProductSearchDto;
import com.nyyb.nyybserver.product.data.dto.response.ProductSuggestionDto;
import com.nyyb.nyybserver.product.data.entity.Product;
import com.nyyb.nyybserver.product.data.repository.ProductRepository;
import com.nyyb.nyybserver.product.data.repository.ProductSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * 제품 마스터 이름 검색.
 * 검색어를 공백으로 쪼개 모든 토큰이 제품 검색어(search_key)에 들어 있는 제품을 찾는다. ("sott 크림" → sott*크림)
 * 조건 구성은 {@link ProductSpecifications} 참고.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private static final int MAX_TOKENS = 5;
    private static final int SUGGESTION_MAX_LIMIT = 20;

    private final ProductRepository productRepository;

    /**
     * 검색창 자동완성. 검색과 같은 조건으로 찾되 목록에 필요한 id·이름만 가져온다.
     * 입력 한 글자마다 호출될 수 있어 건수를 {@value #SUGGESTION_MAX_LIMIT} 건으로 제한한다.
     */
    @Transactional(readOnly = true)
    public List<ProductSuggestionDto> suggest(String keyword, int limit) {
        List<String> tokens = tokenize(keyword);
        if (tokens.isEmpty()) {
            return List.of();
        }

        int capped = Math.clamp(limit, 1, SUGGESTION_MAX_LIMIT);
        return productRepository.findSuggestions(
                ProductSpecifications.searchKeyContainsAllTokens(tokens), capped);
    }

    @Transactional(readOnly = true)
    public List<ProductSearchDto> search(String keyword, Pageable pageable) {
        List<String> tokens = tokenize(keyword);
        if (tokens.isEmpty()) {
            return List.of();
        }

        Page<Product> found = productRepository.findAll(
                ProductSpecifications.searchKeyContainsAllTokens(tokens), pageable);

        return found.getContent().stream()
                .map(ProductSearchDto::from)
                .toList();
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
