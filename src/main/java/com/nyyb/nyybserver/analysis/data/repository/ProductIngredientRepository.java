package com.nyyb.nyybserver.analysis.data.repository;

import com.nyyb.nyybserver.analysis.data.entity.ProductIngredient;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductIngredientRepository extends JpaRepository<ProductIngredient, Long> {
}
