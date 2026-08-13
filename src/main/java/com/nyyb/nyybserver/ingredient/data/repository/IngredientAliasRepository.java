package com.nyyb.nyybserver.ingredient.data.repository;

import com.nyyb.nyybserver.ingredient.data.entity.IngredientAlias;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IngredientAliasRepository extends JpaRepository<IngredientAlias, Long> {

    @Query("""
            select alias
            from IngredientAlias alias
            join fetch alias.ingredient
            """)
    List<IngredientAlias> findAllWithIngredient();
}
