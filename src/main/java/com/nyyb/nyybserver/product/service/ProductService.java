package com.nyyb.nyybserver.product.service;

import com.nyyb.nyybserver.product.data.dto.response.ProductSearchDto;
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

    private final ProductRepository productRepository;

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
