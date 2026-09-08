package com.nyyb.nyybserver.product.data.entity;

import com.nyyb.nyybserver.product.data.converter.CategoryMainConverter;
import com.nyyb.nyybserver.product.data.converter.CategorySubConverter;
import com.nyyb.nyybserver.product.data.enums.CategoryMain;
import com.nyyb.nyybserver.product.data.enums.CategorySub;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 화장품 제품 마스터(쇼핑몰 수집 데이터 기준).
 * 컬럼 구성·이름·타입은 sott 스키마의 product 테이블 정의를 그대로 따른다. (DB가 기준, 애플리케이션이 스키마를 만들지 않는다)
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
    private static final String UNNAMED = "이름 미상 제품";

    // 수집 파이프라인이 부여한 값이라 애플리케이션이 생성하지 않는다.
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "source", nullable = false, length = 32)
    private String source; // 수집처 (amoremall, innisfree, ...)

    @Column(name = "source_key", nullable = false, length = 128)
    private String sourceKey; // 쇼핑몰 내부 상품 ID

    @Column(name = "url", length = 1000)
    private String url; // 상품 상세 페이지 주소

    @Column(name = "brand", length = 200)
    private String brand; // 브랜드명

    @Column(name = "name", length = 500)
    private String name; // 쇼핑몰 표기 그대로의 제품명

    @Column(name = "name_ko", length = 500)
    private String nameKo; // 한글 제품명 (검색용)

    // native=원래 한글 / mfds=식약처 공식명 / translit=영문에서 우리가 생성
    @Column(name = "name_ko_src", columnDefinition = "enum('native','mfds','translit')")
    private String nameKoSrc; // 한글 제품명의 출처

    @Column(name = "search_key", columnDefinition = "TEXT")
    private String searchKey; // 부분검색용: 한글+영문+브랜드를 합친 문자열

    @Column(name = "ingredients", columnDefinition = "LONGTEXT")
    private String ingredients; // 파싱된 전성분 배열 (함량 순서 유지, JSON 원문)

    @Column(name = "verdict", columnDefinition = "enum('ok','low_match','no_text','parse_failed','not_cosmetic')")
    private String verdict; // 전성분 파싱 결과 판정

    @Column(name = "via", columnDefinition = "enum('html','ocr')")
    private String via; // 성분 출처: HTML 텍스트 / 이미지 OCR

    @Column(name = "fetched_at")
    private LocalDateTime fetchedAt; // 수집 시각

    @Convert(converter = CategoryMainConverter.class)
    @Column(name = "category_main", columnDefinition = CategoryMain.COLUMN_DEFINITION)
    private CategoryMain categoryMain; // 카테고리 대분류

    @Convert(converter = CategorySubConverter.class)
    @Column(name = "category_sub", columnDefinition = CategorySub.COLUMN_DEFINITION)
    private CategorySub categorySub; // 카테고리 소분류

    // 분류가 없는 행이 있어 라벨을 뽑는 곳마다 방어하지 않도록 여기서 처리한다.
    public String getCategoryMainLabel() {
        return categoryMain == null ? UNCLASSIFIED : categoryMain.getDbValue();
    }

    public String getCategorySubLabel() {
        return categorySub == null ? UNCLASSIFIED : categorySub.getDbValue();
    }

    /**
     * 화면·프롬프트에 쓰는 제품명. 한글 제품명(name_ko)을 쓰되, 아직 채워지지 않은 행은
     * 쇼핑몰 표기(name)로 대신한다. 둘 다 비어 있는 행이 있어 이름을 뽑는 곳마다 방어하지 않도록
     * 카테고리 라벨과 마찬가지로 여기서 기본값을 채운다.
     */
    public String getDisplayName() {
        if (nameKo != null && !nameKo.isBlank()) {
            return nameKo.strip();
        }
        if (name != null && !name.isBlank()) {
            return name.strip();
        }
        return UNNAMED;
    }
}
