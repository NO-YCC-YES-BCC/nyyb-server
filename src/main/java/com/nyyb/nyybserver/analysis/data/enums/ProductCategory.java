package com.nyyb.nyybserver.analysis.data.enums;

import lombok.Getter;

import java.util.Collections;
import java.util.List;

/**
 * OCR 원문으로 판별하는 제품 카테고리. 13개로 고정한다.
 * 선언 순서는 루틴에서 바르는 순서를 따르며, 매칭은 선언 순서가 아니라
 * "가장 긴 키워드가 걸린 카테고리"로 정한다. (예: "아이크림"은 EYE_CREAM의 '아이크림'이
 * CREAM의 '크림'보다 길어서 EYE_CREAM, "클렌징오일"은 CLEANSER의 '클렌징오일'이 OIL의 '오일'보다 길어서 CLEANSER)
 * 각 카테고리는 루틴에서의 역할 단위인 {@link CategoryGroup}에 속한다.
 */
@Getter
public enum ProductCategory {

    TONER(
            "토너",
            "Toner",
            CategoryGroup.SKIN_CARE,
            List.of("토너", "toner", "닦토", "토너패드", "tonerpad", "닦토패드", "리프레싱", "refreshing",
                    "클리어링", "clearing", "페이셜토너")
    ),

    SKIN(
            "스킨",
            "Skin",
            CategoryGroup.SKIN_CARE,
            List.of("스킨", "skin", "스킨패드", "skinpad", "소프너", "softener", "워터", "water",
                    "스킨워터", "부스터", "booster", "부스팅", "boosting", "플루이드", "fluid",
                    "미스트", "mist", "페이스미스트", "face mist")
    ),

    ESSENCE(
            "에센스",
            "Essence",
            CategoryGroup.SKIN_CARE,
            List.of("에센스", "essence", "퍼스트에센스", "first essence", "트리트먼트", "treatment")
    ),

    SERUM(
            "세럼",
            "Serum",
            CategoryGroup.SKIN_CARE,
            List.of("세럼", "serum", "컨센트레이트", "concentrate")
    ),

    AMPOULE(
            "앰플",
            "Ampoule",
            CategoryGroup.SKIN_CARE,
            List.of("앰플", "ampoule", "ampul", "앰플러", "ampouler", "샷", "shot",
                    "스팟", "spot", "스팟카밍", "스팟세럼", "스팟에센스")
    ),

    LOTION(
            "로션/에멀전",
            "Lotion",
            CategoryGroup.SKIN_CARE,
            List.of("로션", "lotion", "에멀전", "emulsion", "에멀젼", "모이스처라이저", "moisturizer", "밀크", "milk")
    ),

    CREAM(
            "크림",
            "Cream",
            CategoryGroup.SKIN_CARE,
            List.of("크림", "cream", "수분크림", "영양크림", "보습크림", "진정크림", "시카크림",
                    "젤크림", "gel cream", "수딩젤", "soothing gel")
    ),

    EYE_CREAM(
            "아이크림",
            "Eye Cream",
            CategoryGroup.SKIN_CARE,
            List.of("아이크림", "eye cream", "아이세럼", "eye serum", "아이에센스", "eye essence",
                    "아이패치", "eye patch", "아이밤", "eye balm")
    ),

    OIL(
            "페이스오일/오일",
            "Oil",
            CategoryGroup.SKIN_CARE,
            List.of("오일", "oil", "페이스오일", "face oil", "아르간", "페이셜오일")
    ),

    SUNSCREEN(
            "선케어",
            "Sunscreen",
            CategoryGroup.PROTECTION,
            List.of("선크림", "suncream", "선쿠션", "suncushion", "선블록", "sunblock", "선스틱", "sunstick",
                    "선로션", "sunlotion", "선에센스", "sunessence", "선세럼", "sunserum", "선젤", "sungel",
                    "선플루이드", "sunfluid", "sunscreen", "자외선차단", "uv", "spf", "pa++")
    ),

    // 클렌징 오일·워터·폼, 각질 케어까지 세안 제품은 모두 여기로 모은다.
    CLEANSER(
            "클렌저/세안제",
            "Cleanser",
            CategoryGroup.CLEANSING,
            List.of("클렌징", "cleansing", "클렌저", "클렌져", "cleanser", "폼", "foam", "비누", "soap",
                    "워시", "wash", "세안제",
                    "클렌징오일", "cleansing oil", "클렌징밤", "cleansing balm", "클렌징크림", "cleansing cream",
                    "메이크업리무버", "makeup remover", "포인트리무버", "립앤아이",
                    "클렌징워터", "cleansing water", "미셀라", "micellar", "클렌징티슈", "cleansing tissue",
                    "클렌징와이프", "cleansing wipe",
                    "클렌징폼", "cleansing foam", "폼클렌징", "폼클렌저", "폼클렌져", "클렌징젤", "cleansing gel",
                    "페이스워시", "face wash", "페이셜워시", "facial wash", "버블폼", "휘핑폼", "약산성폼",
                    "필링", "peeling", "각질", "각질제거", "스크럽", "scrub", "고마쥬", "gommage",
                    "엑스폴리에이팅", "exfoliating", "exfoliator", "딥클렌징", "deep cleansing")
    ),

    // 시트/슬리핑팩과 트러블 패치처럼 붙였다 떼는 제품
    MASK(
            "마스크/팩",
            "Mask",
            CategoryGroup.ADDITIONAL,
            List.of("마스크", "mask", "마스크팩", "maskpack", "시트마스크", "sheet mask", "팩", "pack",
                    "슬리핑팩", "sleeping pack", "워시오프", "wash off", "모델링", "modeling",
                    "패치", "patch", "트러블패치", "trouble patch", "여드름패치", "스팟패치", "하이드로콜로이드")
    ),

    ETC(
            "기타",
            "Etc",
            CategoryGroup.ETC,
            Collections.emptyList()
    );

    private final String korName;
    private final String engName;
    private final CategoryGroup group;
    private final List<String> keywords;

    ProductCategory(String korName, String engName, CategoryGroup group, List<String> keywords) {
        this.korName = korName;
        this.engName = engName;
        this.group = group;
        this.keywords = keywords;
    }

    // "CLEANSER (클렌저/세안제 · 그룹: 세안)" — LLM 요청 메시지에 넣는 카테고리 표기
    public String describe() {
        return name() + " (" + korName + " · 그룹: " + group.getKorName() + ")";
    }

    public static ProductCategory classify(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return ETC;
        }

        String normalizedText = normalize(ocrText);

        // 가장 긴 키워드가 걸린 카테고리 = 가장 구체적인 카테고리. 길이가 같으면 먼저 선언한 쪽이 이긴다.
        ProductCategory matched = ETC;
        int matchedLength = 0;

        for (ProductCategory category : values()) {
            int length = category.longestMatchLength(normalizedText);
            if (length > matchedLength) {
                matched = category;
                matchedLength = length;
            }
        }

        return matched;
    }

    // 본문에 걸린 키워드 중 가장 긴 것의 길이. 하나도 안 걸리면 0
    private int longestMatchLength(String normalizedText) {
        return keywords.stream()
                .map(ProductCategory::normalize)
                .filter(normalizedText::contains)
                .mapToInt(String::length)
                .max()
                .orElse(0);
    }

    private static String normalize(String text) {
        return text.replaceAll("\\s+", "").toLowerCase();
    }
}
