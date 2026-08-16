package com.nyyb.nyybserver.analysis.data.repository;

import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {

    // 성분 fetch join
    @Query("select pi from ProductIngredient pi " +
            "left join fetch pi.ingredient " +
            "where pi.product.id = :productId")
    List<ProductIngredient> findByProductIdWithIngredient(@Param("productId") Long productId);
}
