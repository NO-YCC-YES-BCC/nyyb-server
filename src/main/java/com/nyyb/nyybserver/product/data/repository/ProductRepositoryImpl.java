package com.nyyb.nyybserver.product.data.repository;

import com.nyyb.nyybserver.product.data.dto.response.ProductSuggestionDto;
import com.nyyb.nyybserver.product.data.entity.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * 자동완성 전용 조회.
 *
 * Product 는 전성분 원문(ingredients, LONGTEXT)과 검색어(search_key, TEXT)를 함께 들고 있어
 * 엔티티로 읽으면 한 건마다 대용량 컬럼이 딸려온다. 자동완성은 입력 한 글자마다 호출되는 자리라
 * id·이름 컬럼만 골라 select 한다.
 */
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepositoryCustom {

    private final EntityManager entityManager;

    @Override
    public List<ProductSuggestionDto> findSuggestions(Specification<Product> spec, int limit) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<Product> root = query.from(Product.class);

        query.multiselect(
                root.get("id").alias("id"),
                root.get("nameKo").alias("nameKo"),
                root.get("name").alias("name"),
                root.get("brand").alias("brand")
        );

        Predicate predicate = spec.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }

        // 짧은 이름을 먼저 보여준다. 같은 검색어에 "수분크림"과 "수분크림 대용량 리필 세트"가 걸리면
        // 사용자가 찾는 쪽은 대체로 짧은 이름이라 목록 위로 올린다.
        Expression<String> displayName = cb.coalesce(root.get("nameKo"), root.get("name"));
        query.orderBy(cb.asc(cb.length(displayName)), cb.asc(root.get("id")));

        return entityManager.createQuery(query)
                .setMaxResults(limit)
                .getResultList()
                .stream()
                .map(row -> new ProductSuggestionDto(
                        row.get("id", Long.class),
                        Product.displayNameOf(row.get("nameKo", String.class), row.get("name", String.class)),
                        row.get("brand", String.class)
                ))
                .toList();
    }
}
