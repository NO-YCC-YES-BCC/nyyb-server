package com.nyyb.nyybserver.product.data.enums;

import lombok.Getter;

/**
 * 제품 마스터(product 테이블)의 CATEGORY_SUB(카테고리 소분류) 컬럼 값.
 * 소분류는 대분류에 종속되지 않는다. 예를 들어 '오일'은 스킨케어·클렌징·바디케어에,
 * '미스트'는 스킨케어·바디케어에 함께 쓰이므로 상위 대분류를 상수에 묶어두지 않는다.
 * DB가 enum 타입으로 값 집합을 고정하고 있어, 선언 순서·표기는 DB 정의를 따른다.
 * 다만 DB 정의에는 있으나 쓰이지 않는 값(디바이스·툴류, 젤네일, 펌, 입욕, 유아동 등)은 상수로 두지 않는다.
 * 홈프래그런스는 해당 행이 없지만 예외로 남겨 둔다.
 * 저장 값은 한글 라벨이므로 CategorySubConverter가 enum 상수와 서로 변환한다.
 */
@Getter
public enum CategorySub {

    SKIN("스킨"),
    TONER("토너"),
    ESSENCE("에센스"),
    SERUM("세럼"),
    AMPOULE("앰플"),
    CREAM("크림"),
    LOTION("로션"),
    MIST("미스트"),
    OIL("오일"),
    SKIN_CARE_SET("스킨케어세트"),
    SHEET_PACK("시트팩"),
    PAD("패드"),
    FACIAL_PACK("페이셜팩"),
    PATCH("패치"),
    CLEANSING_FOAM("클렌징폼"),
    BALM("클렌징밤"),
    WATER("클렌징워터"),
    PEELING("필링"),
    SCRUB("스크럽"),
    REMOVER("리무버"),
    SUN_CREAM("선크림"),
    SUN_STICK("선스틱"),
    SUN_CUSHION("선쿠션"),
    SUN_SPRAY("선스프레이"),
    SUN_PATCH("선패치"),
    LIP_MAKEUP("립메이크업"),
    BASE_MAKEUP("베이스메이크업"),
    EYE_MAKEUP("아이메이크업"),
    FACE_TOOL("페이스 툴"),
    NORMAL_NAIL("일반네일"),
    NAIL_CARE("네일케어"),
    SHAMPOO("샴푸"),
    TREATMENT("트리트먼트"),
    HAIR_PACK("헤어팩"),
    SCALP_ESSENCE("두피에센스"),
    HAIR_ESSENCE("헤어에센스"),
    HAIR_DYE("염모제"),
    STYLING("스타일링"),
    SHOWER("샤워"),
    BODY_LOTION("바디로션"),
    BODY_CREAM("바디크림"),
    DEODORANT("데오드란트"),
    HAND_CARE("핸드케어"),
    FOOT_CARE("풋케어"),
    MINI_PERFUME("미니향수"),
    SOLID_PERFUME("고체향수"),
    HOME_FRAGRANCE("홈프래그런스"),
    PERFUME("향수");

    // DDL의 enum(...) 정의 원문. 실제 컬럼 정의와 어긋나면 ddl-auto가 컬럼을 건드리므로
    // 위 상수 목록이 아니라 DB 정의를 그대로 유지한다. Product 엔티티의 columnDefinition으로 쓴다.
    public static final String COLUMN_DEFINITION =
            "enum('스킨','토너','에센스','세럼','앰플','크림','로션','미스트','오일','스킨케어세트','스킨케어 디바이스','시트팩','패드','페이셜팩','코팩','패치','클렌징폼',"
            + "'젤','밤','워터','밀크','필링','스크럽','티슈','리무버','클렌징 디바이스','선크림','선스틱','선쿠션','선스프레이','선패치','태닝','애프터선','립메이크업',"
            + "'베이스메이크업','아이메이크업','메이크업 툴','아이래쉬 툴','페이스 툴','헤어 툴','바디 툴','데일리 툴','일반네일','젤네일','네일팁','스티커','네일케어','샴푸',"
            + "'스케일러','트리트먼트','헤어팩','두피에센스','헤어에센스','염모제','펌','헤어기기','브러시','스타일링','샤워','입욕','바디로션','바디크림','제모','왁싱','데오드란트',"
            + "'핸드케어','풋케어','유아동','임산부','미니향수','고체향수','홈프래그런스','향수')";

    private final String dbValue; // DB에 저장된 한글 라벨

    CategorySub(String dbValue) {
        this.dbValue = dbValue;
    }

    /**
     * DB 라벨로 상수를 찾는다. 값이 없거나(NULL) 정의에 없는 라벨이면 null.
     */
    public static CategorySub from(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }
        for (CategorySub value : values()) {
            if (value.dbValue.equals(dbValue)) {
                return value;
            }
        }
        return null;
    }
}
