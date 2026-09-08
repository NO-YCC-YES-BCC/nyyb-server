package com.nyyb.nyybserver.product.data.entity;

import com.nyyb.nyybserver.product.data.converter.CategoryMainConverter;
import com.nyyb.nyybserver.product.data.converter.CategorySubConverter;
import com.nyyb.nyybserver.product.data.enums.CategoryMain;
import com.nyyb.nyybserver.product.data.enums.CategorySub;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 화장품 제품 마스터(식약처 화장품 표시·광고 실증 자료 기준).
 * 컬럼 구성·이름·타입은 product 테이블 정의를 그대로 따른다. (DB가 기준, 애플리케이션이 스키마를 만들지 않는다)
 * 성분과의 연결은 {@link ProductIngredient}가 product_id로 이 PK를 가리키는 구조 그대로 유지된다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {

    private static final String UNCLASSIFIED = "미분류";

    // EE_DOC_DATA는 <PARAGRAPH>...<![CDATA[문구]]></PARAGRAPH> 형태의 DOC XML 원문이다.
    // 태그까지 그대로 넘기면 프롬프트만 길어지므로 CDATA 안의 문구만 뽑아 쓴다.
    private static final Pattern CDATA_TEXT = Pattern.compile("<!\\[CDATA\\[(.*?)]]>", Pattern.DOTALL);

    // 화장품보고일련번호. 식약처가 부여한 값이라 애플리케이션이 생성하지 않는다.
    @Id
    @Column(nullable = false)
    private Long id;

    @Column(name = "ITEM_NAME", length = 1000)
    private String itemName; // 품목명

    @Column(name = "ITEM_PH", length = 100)
    private String itemPh; // 품목 PH

    @Column(name = "ENTP_NAME", length = 500)
    private String entpName; // 업소명(제조·책임판매업자)

    @Column(name = "ETHANOL_OVER_YN", length = 10)
    private String ethanolOverYn; // 에탄올 4% 초과 여부 (Y/N)

    @Column(name = "EE_CODE", length = 50)
    private String eeCode; // 효능효과 코드

    @Column(name = "EE_NAME", length = 500)
    private String eeName; // 효능효과명

    @Column(name = "SPF", length = 50)
    private String spf; // 자외선차단지수(SPF)

    @Column(name = "PA", length = 20)
    private String pa; // 자외선차단지수(PA) — 0~4 등급값

    @Column(name = "EFFECT_YN1", length = 10)
    private String effectYn1; // 2호 효능효과 — 미백 (Y/N)

    @Column(name = "EFFECT_YN2", length = 10)
    private String effectYn2; // 2호 효능효과 — 주름개선 (Y/N)

    @Column(name = "EFFECT_YN3", length = 10)
    private String effectYn3; // 2호 효능효과 — 자외선차단 (Y/N)

    @Column(name = "WATER_PROOFING_FLAG", length = 10)
    private String waterProofingFlag; // 자외선차단 내수 여부 — 0(없음)/1(내수성)/2(지속내수성)

    @Column(name = "WATER_PROOFING_NAME", length = 50)
    private String waterProofingName; // 내수성 표기 (내수성/지속내수성)

    @Column(name = "EE_DOC_DATA", columnDefinition = "MEDIUMTEXT")
    private String eeDocData; // 효능효과 문서 데이터 (DOC XML 원문)

    @Column(name = "NB_DOC_DATA", columnDefinition = "MEDIUMTEXT")
    private String nbDocData; // 사용상 주의사항(일반) 문서 데이터 (DOC XML 원문)

    @Convert(converter = CategoryMainConverter.class)
    @Column(name = "CATEGORY_MAIN", columnDefinition = CategoryMain.COLUMN_DEFINITION)
    private CategoryMain categoryMain; // 카테고리 대분류

    @Convert(converter = CategorySubConverter.class)
    @Column(name = "CATEGORY_SUB", columnDefinition = CategorySub.COLUMN_DEFINITION)
    private CategorySub categorySub; // 카테고리 소분류

    // DB가 current_timestamp로 채우고 갱신한다.
    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    // 분류가 없는 행(19.5만 건 중 1만여 건)이 있어 라벨을 뽑는 곳마다 방어하지 않도록 여기서 처리한다.
    public String getCategoryMainLabel() {
        return categoryMain == null ? UNCLASSIFIED : categoryMain.getDbValue();
    }

    public String getCategorySubLabel() {
        return categorySub == null ? UNCLASSIFIED : categorySub.getDbValue();
    }

    /**
     * 효능효과 문서(EE_DOC_DATA)에서 문구만 뽑아 반환한다.
     * 원문이 없거나 CDATA가 하나도 없으면 빈 목록.
     */
    public List<String> getEffectTexts() {
        if (eeDocData == null || eeDocData.isBlank()) {
            return List.of();
        }

        return CDATA_TEXT.matcher(eeDocData).results()
                .map(result -> result.group(1).strip())
                .filter(text -> !text.isEmpty())
                .toList();
    }

    // Y/N 문자열 컬럼은 값이 없을 수 있어(NULL) 없으면 false로 본다.
    public boolean isEthanolOver() {
        return isYes(ethanolOverYn);
    }

    public boolean isWhitening() {
        return isYes(effectYn1);
    }

    public boolean isAntiWrinkle() {
        return isYes(effectYn2);
    }

    public boolean isUvProtection() {
        return isYes(effectYn3);
    }

    private static boolean isYes(String flag) {
        return "Y".equalsIgnoreCase(flag);
    }
}
