package com.nyyb.nyybserver.product.data.enums;

import lombok.Getter;

/**
 * 제품 마스터(product 테이블)의 CATEGORY_MAIN(카테고리 대분류) 컬럼 값.
 * 제품의 큰 갈래를 나눈다.
 * DB가 enum 타입으로 값 집합을 고정하고 있어, 선언 순서·표기를 DB 정의 그대로 옮겨 둔다.
 * 저장 값은 한글 라벨이므로 CategoryMainConverter가 enum 상수와 서로 변환한다.
 */
@Getter
public enum CategoryMain {

    SKIN_CARE("스킨케어"),
    MASK_PACK("마스크팩"),
    CLEANSING("클렌징"),
    SUN_CARE("선케어"),
    MAKEUP("메이크업"),
    BEAUTY_TOOL("뷰티소품"),
    NAIL("네일"),
    HAIR_CARE("헤어케어"),
    BODY_CARE("바디케어"),
    PERFUME("향수"),
    DIFFUSER("디퓨저");

    // DDL의 enum(...) 정의 원문. Product 엔티티의 columnDefinition으로 그대로 쓴다.
    public static final String COLUMN_DEFINITION =
            "enum('스킨케어','마스크팩','클렌징','선케어','메이크업','뷰티소품','네일','헤어케어','바디케어','향수','디퓨저')";

    private final String dbValue; // DB에 저장된 한글 라벨

    CategoryMain(String dbValue) {
        this.dbValue = dbValue;
    }

    /**
     * DB 라벨로 상수를 찾는다. 값이 없거나(NULL) 정의에 없는 라벨이면 null.
     */
    public static CategoryMain from(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }
        for (CategoryMain value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        return null;
    }
}
