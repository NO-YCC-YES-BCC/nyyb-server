package com.nyyb.nyybserver.product.data.repository;

import com.nyyb.nyybserver.product.data.entity.ProductIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {

    // 성분 fetch join
    @Query("select pi from ProductIngredient pi " +
            "left join fetch pi.ingredient " +
            "where pi.product.id = :productId")
    List<ProductIngredient> findByProductIdWithIngredient(@Param("productId") Long productId);

    // 전성분을 다시 조회할 때 기존 매핑을 걷어낸다
    void deleteByProductId(Long productId);

    // 복수 제품의 성분을 한 번에 조회 (제품별 매칭 응답용, N+1 방지)
    @Query("select pi from ProductIngredient pi " +
            "join fetch pi.product " +
            "left join fetch pi.ingredient " +
            "where pi.product.id in :productIds")
    List<ProductIngredient> findByProductIdInWithIngredient(@Param("productIds") Collection<Long> productIds);
}
