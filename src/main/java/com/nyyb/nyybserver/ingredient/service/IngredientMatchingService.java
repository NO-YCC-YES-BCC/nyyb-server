package com.nyyb.nyybserver.ingredient.service;

import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientMatchCandidateDto;
import com.nyyb.nyybserver.ingredient.data.dto.response.IngredientMatchResult;
import com.nyyb.nyybserver.ingredient.data.enums.IngredientMatchStatus;
import com.nyyb.nyybserver.ingredient.data.enums.IngredientMatchType;
import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import com.nyyb.nyybserver.ingredient.service.IngredientSearchCache.CachedIngredientMatch;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngredientMatchingService {

    private final IngredientNameNormalizer ingredientNameNormalizer;
    private final IngredientSearchCache ingredientSearchCache;

    public List<IngredientMatchResult> match(List<String> inputNames) {
        List<IngredientSearchKey> searchKeys = buildSearchKeys(inputNames);
        if (searchKeys.isEmpty()) {
            return List.of();
        }

        Set<Long> emittedIngredientIds = new HashSet<>();
        Set<String> emittedUnresolvedKeys = new HashSet<>();
        List<IngredientMatchResult> results = new ArrayList<>();

        for (IngredientSearchKey searchKey : searchKeys) {
            IngredientMatchResult result = matchOne(searchKey);
            if (shouldEmit(result, searchKey.normalizedName(), emittedIngredientIds, emittedUnresolvedKeys)) {
                results.add(result);
            }
        }

        return results;
    }

    private List<IngredientSearchKey> buildSearchKeys(List<String> inputNames) {
        if (inputNames == null || inputNames.isEmpty()) {
            return List.of();
        }

        List<IngredientSearchKey> searchKeys = new ArrayList<>();
        for (String inputName : inputNames) {
            String normalizedName = ingredientNameNormalizer.normalize(inputName);
            if (!StringUtils.hasText(normalizedName)) {
                continue;
            }
            searchKeys.add(new IngredientSearchKey(inputName.trim(), normalizedName));
        }

        return searchKeys;
    }

    private IngredientMatchResult matchOne(IngredientSearchKey searchKey) {
        List<CachedIngredientMatch> canonicalMatches = ingredientSearchCache.findByCanonicalName(searchKey.normalizedName());
        if (!canonicalMatches.isEmpty()) {
            return toResult(searchKey, IngredientMatchType.CANONICAL_NAME, canonicalMatches);
        }

        List<CachedIngredientMatch> aliasMatches = ingredientSearchCache.findByAlias(searchKey.normalizedName());
        if (!aliasMatches.isEmpty()) {
            return toResult(searchKey, IngredientMatchType.ALIAS, aliasMatches);
        }

        return IngredientMatchResult.builder()
                .inputName(searchKey.inputName())
                .matchStatus(IngredientMatchStatus.NOT_FOUND)
                .matchType(IngredientMatchType.NONE)
                .ingredientId(null)
                .ingredientName(null)
                .matchedAlias(null)
                .isToxic(null)
                .riskLevel(null)
                .riskLabel("규제 DB에서 발견되지 않음")
                .regulated(false)
                .description("현재 DB에서 일치하는 성분을 찾지 못했습니다. 안전함을 의미하지 않습니다.")
                .candidates(List.of())
                .ambiguousCandidates(List.of())
                .build();
    }

    private IngredientMatchResult toResult(
            IngredientSearchKey searchKey,
            IngredientMatchType matchType,
            List<CachedIngredientMatch> matches
    ) {
        if (matches.size() > 1) {
            List<IngredientMatchCandidateDto> candidates = matches.stream()
                    .map(this::toCandidate)
                    .toList();
            return IngredientMatchResult.builder()
                    .inputName(searchKey.inputName())
                    .matchStatus(IngredientMatchStatus.AMBIGUOUS)
                    .matchType(matchType)
                    .ingredientId(null)
                    .ingredientName(null)
                    .matchedAlias(null)
                    .isToxic(null)
                    .riskLevel(null)
                    .riskLabel("후보가 여러 개라 위험도 확정 불가")
                    .regulated(false)
                    .description("동일한 정규화 성분명이 여러 성분과 일치합니다. 임의로 하나를 선택하지 않았습니다.")
                    .candidates(candidates)
                    .ambiguousCandidates(candidates)
                    .build();
        }

        CachedIngredientMatch match = matches.get(0);
        return IngredientMatchResult.builder()
                .inputName(searchKey.inputName())
                .matchStatus(IngredientMatchStatus.MATCHED)
                .matchType(matchType)
                .ingredientId(match.ingredientId())
                .ingredientName(match.ingredientName())
                .matchedAlias(matchType == IngredientMatchType.ALIAS ? match.matchedAlias() : null)
                .isToxic(match.isToxic())
                .riskLevel(match.riskLevel())
                .riskLabel(riskLabel(match.riskLevel()))
                .regulated(isRegulated(match))
                .description(match.description())
                .candidates(List.of(toCandidate(match)))
                .ambiguousCandidates(List.of())
                .build();
    }

    private boolean shouldEmit(
            IngredientMatchResult result,
            String normalizedName,
            Set<Long> emittedIngredientIds,
            Set<String> emittedUnresolvedKeys
    ) {
        if (result.getMatchStatus() == IngredientMatchStatus.MATCHED) {
            return emittedIngredientIds.add(result.getIngredientId());
        }
        return emittedUnresolvedKeys.add(result.getMatchStatus().name() + ":" + normalizedName);
    }

    private IngredientMatchCandidateDto toCandidate(CachedIngredientMatch match) {
        return IngredientMatchCandidateDto.builder()
                .ingredientId(match.ingredientId())
                .ingredientName(match.ingredientName())
                .isToxic(match.isToxic())
                .riskLevel(match.riskLevel())
                .riskLabel(riskLabel(match.riskLevel()))
                .regulated(isRegulated(match))
                .description(match.description())
                .build();
    }

    private boolean isRegulated(CachedIngredientMatch match) {
        return Boolean.TRUE.equals(match.isToxic())
                || match.riskLevel() == RiskLevel.MEDIUM
                || match.riskLevel() == RiskLevel.HIGH
                || match.riskLevel() == RiskLevel.DISALLOWED;
    }

    private String riskLabel(RiskLevel riskLevel) {
        if (riskLevel == null) {
            return "위험도 미분류";
        }

        return switch (riskLevel) {
            case DISALLOWED -> "사용금지";
            case HIGH -> "고위험";
            case MEDIUM -> "사용제한 또는 주의 필요";
            case LOW -> "저위험으로 분류된 성분";
        };
    }

    private record IngredientSearchKey(String inputName, String normalizedName) {
    }
}
