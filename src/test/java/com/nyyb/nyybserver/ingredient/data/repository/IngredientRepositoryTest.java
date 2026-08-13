package com.nyyb.nyybserver.ingredient.data.repository;

import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.entity.IngredientAlias;
import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.datasource.url=jdbc:h2:mem:ingredient-repository-test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ContextConfiguration(classes = IngredientRepositoryTest.JpaTestConfig.class)
class IngredientRepositoryTest {

    private final TestEntityManager entityManager;
    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository ingredientAliasRepository;

    @Autowired
    IngredientRepositoryTest(
            TestEntityManager entityManager,
            IngredientRepository ingredientRepository,
            IngredientAliasRepository ingredientAliasRepository
    ) {
        this.entityManager = entityManager;
        this.ingredientRepository = ingredientRepository;
        this.ingredientAliasRepository = ingredientAliasRepository;
    }

    @Test
    void loadsAllIngredientsForCache() {
        Ingredient ingredient = persistIngredient("리날룰", false, RiskLevel.MEDIUM);
        entityManager.flush();
        entityManager.clear();

        List<Ingredient> results = ingredientRepository.findAll();

        assertThat(results).extracting(Ingredient::getId).containsExactly(ingredient.getId());
        assertThat(results.get(0).getName()).isEqualTo("리날룰");
    }

    @Test
    void loadsAllAliasesWithIngredientForCacheWithoutLazyLookup() {
        Ingredient ingredient = persistIngredient("리날룰", false, RiskLevel.MEDIUM);
        IngredientAlias alias = IngredientAlias.builder()
                .ingredient(ingredient)
                .alias("Linalool")
                .build();
        entityManager.persist(alias);
        entityManager.flush();
        entityManager.clear();

        List<IngredientAlias> results = ingredientAliasRepository.findAllWithIngredient();

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getAlias()).isEqualTo("Linalool");
        assertThat(results.get(0).getIngredient().getName()).isEqualTo("리날룰");
    }

    private Ingredient persistIngredient(String name, boolean isToxic, RiskLevel riskLevel) {
        Ingredient ingredient = Ingredient.builder()
                .name(name)
                .isToxic(isToxic)
                .riskLevel(riskLevel)
                .description("description")
                .build();
        entityManager.persist(ingredient);
        return ingredient;
    }

    @Configuration
    @EntityScan(basePackageClasses = {
            Ingredient.class,
            IngredientAlias.class
    })
    @EnableJpaRepositories(basePackageClasses = {
            IngredientRepository.class,
            IngredientAliasRepository.class
    })
    static class JpaTestConfig {
    }
}
