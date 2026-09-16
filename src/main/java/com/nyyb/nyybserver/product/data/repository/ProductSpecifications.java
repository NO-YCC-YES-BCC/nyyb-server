package com.nyyb.nyybserver.product.data.repository;

import com.nyyb.nyybserver.product.data.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * 제품 마스터 검색 조건.
 *
 * 수집 데이터는 제품명이 쇼핑몰 표기(name)·한글명(name_ko)·브랜드(brand)로 흩어져 있고,
 * search_key가 이 셋에 로마자 전사까지 합쳐 둔 부분검색용 컬럼이라 매칭은 search_key만 본다.
 * 사용자가 "브랜드명 크림"처럼 띄어 입력하면 검색어를 공백으로 쪼갠 뒤 모든 토큰이 들어 있는 제품을 찾는다. (AND 조건)
 *
 * search_key는 TEXT라 LIKE '%...%'가 전체 스캔이 되지만, 제품 마스터가 수천 건 규모라 그대로 둔다.
 * 규모가 커지면 search_key에 FULLTEXT 인덱스를 걸고 MATCH...AGAINST로 바꾸는 편이 낫다.
 */
public final class ProductSpecifications {

    private static final char ESCAPE = '\\';

    private ProductSpecifications() {
    }

    /**
     * 모든 토큰을 search_key 포함으로 매칭한다.
     */
    public static Specification<Product> searchKeyContainsAllTokens(List<String> tokens) {
        return (root, query, cb) -> {
            List<Predicate> predicates = tokens.stream()
                    .map(token -> (Predicate) cb.like(root.get("searchKey"), "%" + escape(token) + "%", ESCAPE))
                    .toList();
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    // LIKE 와일드카드(%, _)와 이스케이프 문자 자체를 사용자 입력에서 무력화한다.
    private static String escape(String token) {
        return token
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
