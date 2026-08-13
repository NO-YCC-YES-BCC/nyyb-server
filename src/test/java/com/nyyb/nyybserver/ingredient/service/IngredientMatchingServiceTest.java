package com.nyyb.nyybserver.ingredient.service;

import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientMatchResult;
import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.entity.IngredientAlias;
import com.nyyb.nyybserver.ingredient.data.enums.IngredientMatchStatus;
import com.nyyb.nyybserver.ingredient.data.enums.IngredientMatchType;
import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import com.nyyb.nyybserver.ingredient.data.repository.IngredientAliasRepository;
import com.nyyb.nyybserver.ingredient.data.repository.IngredientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IngredientMatchingServiceTest {

    private IngredientRepository ingredientRepository;
    private IngredientAliasRepository ingredientAliasRepository;
    private IngredientNameNormalizer normalizer;
    private IngredientSearchCache searchCache;
    private IngredientMatchingService matchingService;

    @BeforeEach
    void setUp() {
        ingredientRepository = mock(IngredientRepository.class);
        ingredientAliasRepository = mock(IngredientAliasRepository.class);
        normalizer = new IngredientNameNormalizer();
        searchCache = new IngredientSearchCache(ingredientRepository, ingredientAliasRepository, normalizer);
        matchingService = new IngredientMatchingService(normalizer, searchCache);
    }

    @Test
    void matchesKoreanCanonicalName() {
        Ingredient ingredient = ingredient(1L, "리날룰", false, RiskLevel.MEDIUM, "사용제한 원료");
        resetCache(List.of(ingredient), List.of());

        IngredientMatchResult result = matchingService.match(List.of("리날룰")).get(0);

        assertThat(result.getMatchStatus()).isEqualTo(IngredientMatchStatus.MATCHED);
        assertThat(result.getMatchType()).isEqualTo(IngredientMatchType.CANONICAL_NAME);
        assertThat(result.getIngredientId()).isEqualTo(1L);
        assertThat(result.getIngredientName()).isEqualTo("리날룰");
    }

    @Test
    void matchesEnglishAliasIgnoringCase() {
        Ingredient ingredient = ingredient(1L, "리날룰", false, RiskLevel.MEDIUM, "사용제한 원료");
        IngredientAlias alias = alias(1L, ingredient, "Linalool");
        resetCache(List.of(ingredient), List.of(alias));

        IngredientMatchResult result = matchingService.match(List.of("lInAlOoL")).get(0);

        assertThat(result.getMatchStatus()).isEqualTo(IngredientMatchStatus.MATCHED);
        assertThat(result.getMatchType()).isEqualTo(IngredientMatchType.ALIAS);
        assertThat(result.getMatchedAlias()).isEqualTo("Linalool");
        assertThat(result.getIngredientName()).isEqualTo("리날룰");
    }

    @Test
    void matchesKoreanAlias() {
        Ingredient ingredient = ingredient(1L, "프로피오닉애씨드", false, RiskLevel.MEDIUM, "사용조건 확인 필요");
        IngredientAlias alias = alias(1L, ingredient, "프로피온산");
        resetCache(List.of(ingredient), List.of(alias));

        IngredientMatchResult result = matchingService.match(List.of("프로피온산")).get(0);

        assertThat(result.getMatchStatus()).isEqualTo(IngredientMatchStatus.MATCHED);
        assertThat(result.getMatchType()).isEqualTo(IngredientMatchType.ALIAS);
        assertThat(result.getIngredientName()).isEqualTo("프로피오닉애씨드");
    }

    @Test
    void matchesIngredientWithMixedSpacesAndLineBreaks() {
        Ingredient ingredient = ingredient(1L, "프로피오닉애씨드", false, RiskLevel.MEDIUM, null);
        resetCache(List.of(ingredient), List.of());

        IngredientMatchResult result = matchingService.match(List.of(" 프로피오닉\n 애씨드 ")).get(0);

        assertThat(result.getMatchStatus()).isEqualTo(IngredientMatchStatus.MATCHED);
        assertThat(result.getIngredientName()).isEqualTo("프로피오닉애씨드");
    }

    @Test
    void deDuplicatesSameIngredientMatchedByCanonicalNameAndAlias() {
        Ingredient ingredient = ingredient(1L, "리날룰", false, RiskLevel.MEDIUM, null);
        IngredientAlias alias = alias(1L, ingredient, "Linalool");
        resetCache(List.of(ingredient), List.of(alias));

        List<IngredientMatchResult> results = matchingService.match(List.of("리날룰", "Linalool"));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getInputName()).isEqualTo("리날룰");
        assertThat(results.get(0).getMatchType()).isEqualTo(IngredientMatchType.CANONICAL_NAME);
    }

    @Test
    void disallowedRiskLevelMeansRegulatedProhibitedIngredient() {
        Ingredient ingredient = ingredient(1L, "금지원료", true, RiskLevel.DISALLOWED, "화장품 사용금지");
        resetCache(List.of(ingredient), List.of());

        IngredientMatchResult result = matchingService.match(List.of("금지원료")).get(0);

        assertThat(result.getRegulated()).isTrue();
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.DISALLOWED);
        assertThat(result.getRiskLabel()).isEqualTo("사용금지");
    }

    @Test
    void mediumRiskLevelMeansRestrictedOrCautionRequired() {
        Ingredient ingredient = ingredient(1L, "제한원료", false, RiskLevel.MEDIUM, "사용한도 확인");
        resetCache(List.of(ingredient), List.of());

        IngredientMatchResult result = matchingService.match(List.of("제한원료")).get(0);

        assertThat(result.getRegulated()).isTrue();
        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(result.getRiskLabel()).isEqualTo("사용제한 또는 주의 필요");
    }

    @Test
    void nullRiskLevelRemainsUnclassified() {
        Ingredient ingredient = ingredient(1L, "일반원료", false, null, "마스터 원료");
        resetCache(List.of(ingredient), List.of());

        IngredientMatchResult result = matchingService.match(List.of("일반원료")).get(0);

        assertThat(result.getMatchStatus()).isEqualTo(IngredientMatchStatus.MATCHED);
        assertThat(result.getRiskLevel()).isNull();
        assertThat(result.getRiskLabel()).isEqualTo("위험도 미분류");
        assertThat(result.getRegulated()).isFalse();
    }

    @Test
    void unknownIngredientIsNotFound() {
        resetCache(List.of(), List.of());

        IngredientMatchResult result = matchingService.match(List.of("알 수 없는 성분")).get(0);

        assertThat(result.getMatchStatus()).isEqualTo(IngredientMatchStatus.NOT_FOUND);
        assertThat(result.getMatchType()).isEqualTo(IngredientMatchType.NONE);
        assertThat(result.getDescription()).contains("안전함을 의미하지 않습니다");
    }

    @Test
    void ambiguousAliasReturnsAllCandidatesWithoutChoosingOne() {
        Ingredient first = ingredient(1L, "후보A", false, RiskLevel.LOW, null);
        Ingredient second = ingredient(2L, "후보B", true, RiskLevel.HIGH, null);
        resetCache(List.of(first, second), List.of(
                alias(1L, first, "공통이명"),
                alias(2L, second, "공통이명")
        ));

        IngredientMatchResult result = matchingService.match(List.of("공통이명")).get(0);

        assertThat(result.getMatchStatus()).isEqualTo(IngredientMatchStatus.AMBIGUOUS);
        assertThat(result.getIngredientId()).isNull();
        assertThat(result.getAmbiguousCandidates()).hasSize(2);
    }

    @Test
    void canonicalNameTakesPriorityOverAmbiguousAlias() {
        Ingredient canonical = ingredient(1L, "공통이명", false, null, null);
        Ingredient firstAliasCandidate = ingredient(2L, "후보A", false, RiskLevel.LOW, null);
        Ingredient secondAliasCandidate = ingredient(3L, "후보B", true, RiskLevel.HIGH, null);
        resetCache(List.of(canonical, firstAliasCandidate, secondAliasCandidate), List.of(
                alias(1L, firstAliasCandidate, "공통이명"),
                alias(2L, secondAliasCandidate, "공통이명")
        ));

        IngredientMatchResult result = matchingService.match(List.of("공통이명")).get(0);

        assertThat(result.getMatchStatus()).isEqualTo(IngredientMatchStatus.MATCHED);
        assertThat(result.getMatchType()).isEqualTo(IngredientMatchType.CANONICAL_NAME);
        assertThat(result.getIngredientId()).isEqualTo(1L);
    }

    @Test
    void ignoresNullAndBlankInputs() {
        resetCache(List.of(), List.of());

        assertThat(matchingService.match(null)).isEmpty();
        assertThat(matchingService.match(Arrays.asList(null, "", " ", "\t"))).isEmpty();
    }

    @Test
    void doesNotQueryRepositoryPerIngredientAfterCacheInitialized() {
        resetCache(List.of(
                ingredient(1L, "성분A", false, null, null),
                ingredient(2L, "성분B", false, null, null)
        ), List.of());
        clearInvocations(ingredientRepository, ingredientAliasRepository);

        matchingService.match(List.of("성분A", "성분B", "성분C"));

        verifyNoInteractions(ingredientRepository, ingredientAliasRepository);
    }

    @Test
    void parserDoesNotSplitCommaBetweenDigits() {
        IngredientNameParser parser = new IngredientNameParser();

        List<String> parsed = parser.parse("1,3-Dioxane, 리날룰; 정제수");

        assertThat(parsed).containsExactly("1,3-Dioxane", "리날룰", "정제수");
    }

    private void resetCache(List<Ingredient> ingredients, List<IngredientAlias> aliases) {
        searchCache.resetForTest(ingredients, aliases);
    }

    private Ingredient ingredient(Long id, String name, boolean isToxic, RiskLevel riskLevel, String description) {
        return Ingredient.builder()
                .id(id)
                .name(name)
                .isToxic(isToxic)
                .riskLevel(riskLevel)
                .description(description)
                .build();
    }

    private IngredientAlias alias(Long id, Ingredient ingredient, String alias) {
        return IngredientAlias.builder()
                .id(id)
                .ingredient(ingredient)
                .alias(alias)
                .build();
    }
}
