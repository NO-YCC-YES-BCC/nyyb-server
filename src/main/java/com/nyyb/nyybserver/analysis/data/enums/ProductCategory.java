package com.nyyb.nyybserver.analysis.data.enums;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum ProductCategory {

    TONER(
            "토너",
            "Toner",
            List.of("토너", "toner", "닦토", "토너패드", "tonerpad", "닦토패드", "리프레싱", "refreshing", "클리어링", "clearing", "페이셜토너")
    ),

    SKIN(
            "스킨",
            "Skin",
            List.of("스킨", "skin", "스킨패드", "skinpad", "소프너", "softener", "워터", "water", "스킨워터", "부스터", "booster", "부스팅", "boosting", "플루이드", "fluid")
    ),

    ESSENCE(
            "에센스",
            "Essence",
            List.of("에센스", "essence", "퍼스트에센스", "first essence", "트리트먼트", "treatment")
    ),

    SERUM(
            "세럼",
            "Serum",
            List.of("세럼", "serum", "컨센트레이트", "concentrate")
    ),

    AMPOULE(
            "앰플",
            "Ampoule",
            List.of("앰플", "ampoule", "ampul", "앰플러", "ampouler", "샷", "shot")
    ),

    LOTION(
            "로션/에멀전",
            "Lotion",
            List.of("로션", "lotion", "에멀전", "emulsion", "에멀젼", "모이스처라이저", "moisturizer", "밀크", "milk")
    ),

    CREAM(
            "크림",
            "Cream",
            List.of("크림", "cream", "수분크림", "영양크림", "보습크림", "진정크림", "시카크림", "밤", "balm", "젤크림", "gel cream", "수딩젤", "soothing gel")
    ),

    EYE_CREAM(
            "아이크림",
            "Eye Cream",
            List.of("아이크림", "eye cream", "아이세럼", "eye serum", "아이패치", "eye patch")
    ),

    OIL(
            "페이스오일/오일",
            "Oil",
            List.of("오일", "oil", "페이스오일", "face oil", "아르간", "페이셜오일")
    ),

    SUNSCREEN(
            "선케어",
            "Sunscreen",
            List.of("선크림", "sunscreen", "선블록", "sunblock", "선로션", "sunlotion", "선에센스", "sunessence", "선스틱", "sunstick", "선쿠션", "suncushion", "선플루이드", "sunfluid", "자외선차단", "uv")
    ),

    CLEANSER(
            "클렌저/세안제",
            "Cleanser",
            List.of("클렌징", "cleansing", "클렌저", "cleanser", "폼", "foam", "비누", "soap", "워시", "wash", "클렌징오일", "클렌징폼", "클렌징워터", "클렌징밤", "클렌징티슈", "립앤아이")
    ),

    MASK(
            "마스크/팩",
            "Mask",
            List.of("마스크", "mask", "마스크팩", "maskpack", "시트마스크", "sheet mask", "팩", "pack", "슬리핑팩", "sleeping pack", "워시오프", "wash off", "모델링", "modeling")
    ),

    ETC(
            "기타",
            "Etc",
            Collections.emptyList()
    );

    private final String korName;
    private final String engName;
    private final List<String> keywords;

    ProductCategory(String korName, String engName, List<String> keywords) {
        this.korName = korName;
        this.engName = engName;
        this.keywords = keywords;
    }

    public static ProductCategory classify(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return ETC;
        }

        String normalizedText = ocrText.replaceAll("\\s+", "").toLowerCase();

        // 1. 세부 오버라이드 카테고리 (일반 크림보다 아이크림, 선크림 등을 우선 매칭)
        for (ProductCategory category : Arrays.asList(EYE_CREAM, SUNSCREEN, AMPOULE)) {
            if (category.matches(normalizedText)) {
                return category;
            }
        }

        // 2. 전체 카테고리 순회 검사
        for (ProductCategory category : values()) {
            if (category == ETC) continue;
            if (category.matches(normalizedText)) {
                return category;
            }
        }

        return ETC;
    }

    private boolean matches(String normalizedText) {
        return keywords.stream()
                .anyMatch(keyword -> normalizedText.contains(keyword.toLowerCase()));
    }

    public String getKorName() { return korName; }
    public String getEngName() { return engName; }
    public List<String> getKeywords() { return keywords; }
}