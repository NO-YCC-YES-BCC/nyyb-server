package com.nyyb.nyybserver.ingredient.service;

import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.entity.IngredientAlias;
import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import com.nyyb.nyybserver.ingredient.data.repository.IngredientAliasRepository;
import com.nyyb.nyybserver.ingredient.data.repository.IngredientRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class IngredientSearchCache {

    private final IngredientRepository ingredientRepository;
    private final IngredientAliasRepository ingredientAliasRepository;
    private final IngredientNameNormalizer ingredientNameNormalizer;

    private volatile ConcurrentHashMap<String, List<CachedIngredientMatch>> canonicalNameIndex = new ConcurrentHashMap<>();
    private volatile ConcurrentHashMap<String, List<CachedIngredientMatch>> aliasIndex = new ConcurrentHashMap<>();

    @PostConstruct
    @Transactional(readOnly = true)
    public void refresh() {
        reset(
                ingredientRepository.findAll(),
                ingredientAliasRepository.findAllWithIngredient()
        );
    }

    public List<CachedIngredientMatch> findByCanonicalName(String normalizedName) {
        return canonicalNameIndex.getOrDefault(normalizedName, List.of());
    }

    public List<CachedIngredientMatch> findByAlias(String normalizedAlias) {
        return aliasIndex.getOrDefault(normalizedAlias, List.of());
    }

    public void resetForTest(
            Collection<Ingredient> ingredients,
            Collection<IngredientAlias> aliases
    ) {
        reset(ingredients, aliases);
    }

    private void reset(
            Collection<Ingredient> ingredients,
            Collection<IngredientAlias> aliases
    ) {
        ConcurrentHashMap<String, List<CachedIngredientMatch>> nextCanonicalNameIndex = new ConcurrentHashMap<>();
        ConcurrentHashMap<String, List<CachedIngredientMatch>> nextAliasIndex = new ConcurrentHashMap<>();

        ingredients.forEach(ingredient -> {
            String normalizedName = ingredientNameNormalizer.normalize(ingredient.getName());
            if (StringUtils.hasText(normalizedName)) {
                add(nextCanonicalNameIndex, normalizedName, CachedIngredientMatch.byCanonicalName(ingredient));
            }
        });

        aliases.forEach(alias -> {
            String normalizedAlias = ingredientNameNormalizer.normalize(alias.getAlias());
            if (StringUtils.hasText(normalizedAlias)) {
                add(nextAliasIndex, normalizedAlias, CachedIngredientMatch.byAlias(alias));
            }
        });

        canonicalNameIndex = freeze(nextCanonicalNameIndex);
        aliasIndex = freeze(nextAliasIndex);
    }

    private void add(
            ConcurrentHashMap<String, List<CachedIngredientMatch>> index,
            String normalizedName,
            CachedIngredientMatch match
    ) {
        index.compute(normalizedName, (ignored, existingMatches) -> {
            List<CachedIngredientMatch> matches = existingMatches == null
                    ? new ArrayList<>()
                    : new ArrayList<>(existingMatches);

            boolean alreadyExists = matches.stream()
                    .anyMatch(existing -> Objects.equals(existing.ingredientId(), match.ingredientId()));
            if (!alreadyExists) {
                matches.add(match);
            }
            return matches;
        });
    }

    private ConcurrentHashMap<String, List<CachedIngredientMatch>> freeze(
            ConcurrentHashMap<String, List<CachedIngredientMatch>> source
    ) {
        ConcurrentHashMap<String, List<CachedIngredientMatch>> frozen = new ConcurrentHashMap<>();
        source.forEach((key, value) -> frozen.put(key, List.copyOf(value)));
        return frozen;
    }

    public record CachedIngredientMatch(
            Long ingredientId,
            String ingredientName,
            Boolean isToxic,
            RiskLevel riskLevel,
            String description,
            String matchedAlias
    ) {
        public static CachedIngredientMatch byCanonicalName(Ingredient ingredient) {
            return new CachedIngredientMatch(
                    ingredient.getId(),
                    ingredient.getName(),
                    ingredient.getIsToxic(),
                    ingredient.getRiskLevel(),
                    ingredient.getDescription(),
                    null
            );
        }

        public static CachedIngredientMatch byAlias(IngredientAlias alias) {
            Ingredient ingredient = alias.getIngredient();
            return new CachedIngredientMatch(
                    ingredient.getId(),
                    ingredient.getName(),
                    ingredient.getIsToxic(),
                    ingredient.getRiskLevel(),
                    ingredient.getDescription(),
                    alias.getAlias()
            );
        }
    }
}
