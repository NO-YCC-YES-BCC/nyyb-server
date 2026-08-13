package com.nyyb.nyybserver.ingredient.data.repository;

import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface IngredientRepository extends JpaRepository<Ingredient, Long> {

    @Query("select distinct i from Ingredient i left join fetch i.aliases")
    List<Ingredient> findAllWithAliases();
}
