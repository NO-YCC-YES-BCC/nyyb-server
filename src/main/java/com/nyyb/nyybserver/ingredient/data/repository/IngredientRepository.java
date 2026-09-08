package com.nyyb.nyybserver.ingredient.data.repository;

import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    // 정렬 파라미터를 받지 않으므로 이름 → id 순으로 고정 정렬 (페이지 간 순서 보장)
    List<Ingredient> findAllByOrderByNameAscIdAsc(Pageable pageable);
}
