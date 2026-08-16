package com.nyyb.nyybserver.ingredient.service;

import com.nyyb.nyybserver.ingredient.data.entity.Ingredient;
import com.nyyb.nyybserver.ingredient.data.entity.IngredientAlias;
import com.nyyb.nyybserver.ingredient.data.repository.IngredientRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 성분 이름/이명을 메모리에 올려두고 OCR 토큰을 즉시 매칭하는 인덱스.
 * 서버 시작 시 1번 로드하며, 이후 모든 요청이 이 Map을 공유한다. (DB 인덱스 불필요)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngredientIndex {

    private final IngredientRepository ingredientRepository;

    // key: 정규화된 이름/이명, value: 대표 성분 (이명 여러 개가 같은 성분을 가리킴)
    private Map<String, Ingredient> index = Map.of();

    @PostConstruct
    void load() {
        Map<String, Ingredient> map = new HashMap<>();
        for (Ingredient ingredient : ingredientRepository.findAllWithAliases()) {
            map.put(normalize(ingredient.getName()), ingredient);
            for (IngredientAlias alias : ingredient.getAliases()) {
                map.put(normalize(alias.getAlias()), ingredient);
            }
        }
        this.index = map;
        log.info("성분 인덱스 로드 완료: {} keys", map.size());
    }

    /**
     * OCR 토큰(원문)을 정규화해 매칭되는 성분을 반환한다. 없으면 null.
     */
    public Ingredient match(String rawToken) {
        String key = normalize(rawToken);
        if (key.isEmpty()) {
            return null;
        }
        return index.get(key);
    }

    // 소문자화 + 모든 공백 제거 (저장/조회 양쪽에서 동일하게 사용)
    private String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.toLowerCase().replaceAll("\\s+", "");
    }
}
