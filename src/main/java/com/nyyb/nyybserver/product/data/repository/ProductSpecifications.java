package com.nyyb.nyybserver.product.data.repository;

import com.nyyb.nyybserver.product.data.entity.Product;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * 제품 마스터 검색 조건.
 *
 * ITEM_NAME은 브랜드명·제품 라인명·제형이 띄어쓰기 없이 한 덩어리로 붙어 있어(전체 19.5만 건 중 공백 포함은 539건뿐)
 * 사용자가 "브랜드명 크림"처럼 띄어 입력한 검색어를 통째로 매칭할 수 없다. 그래서 검색어를 공백으로 쪼갠 뒤
 * 모든 토큰이 이름 안에 들어 있는 제품을 찾는다. (AND 조건)
 *
 * 다만 LIKE '%...%'는 인덱스를 못 타서 전체 스캔이 된다. 브랜드명은 거의 항상 이름 맨 앞에 오므로
 * (표본으로 확인한 브랜드들은 해당 제품 대부분이 브랜드명으로 시작했다) 첫 토큰만 접두 매칭으로 걸어 idx_item_name을 태우고
 * 나머지 토큰은 포함 매칭으로 좁힌다. 접두 매칭이 빈손이면 서비스가 전체 포함 매칭으로 다시 찾는다.
 */
public final class ProductSpecifications {

    private static final char ESCAPE = '\\';

    private ProductSpecifications() {
    }

    /**
     * 첫 토큰은 접두(인덱스 사용), 나머지 토큰은 포함으로 매칭한다.
     */
    public static Specification<Product> nameStartsWithFirstToken(List<String> tokens) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.like(root.get("itemName"), escape(tokens.getFirst()) + "%", ESCAPE));
            for (String token : tokens.subList(1, tokens.size())) {
                predicates.add(cb.like(root.get("itemName"), "%" + escape(token) + "%", ESCAPE));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * 모든 토큰을 포함 매칭한다. 브랜드명을 앞에 두지 않고 검색한 경우까지 걸러내는 대신 전체 스캔이 된다.
     */
    public static Specification<Product> nameContainsAllTokens(List<String> tokens) {
        return (root, query, cb) -> {
            List<Predicate> predicates = tokens.stream()
                    .map(token -> cb.like(root.get("itemName"), "%" + escape(token) + "%", ESCAPE))
                    .map(Predicate.class::cast)
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
