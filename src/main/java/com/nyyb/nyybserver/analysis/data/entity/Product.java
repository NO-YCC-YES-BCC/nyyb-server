package com.nyyb.nyybserver.analysis.data.entity;

import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.user.data.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 소유자(게스트/카카오 공통). OCR 업로드 시점의 로그인 유저로 지정, 게스트→소셜 병합 시 재지정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private Analysis analysis;

    @Column(nullable = false)
    private String imageKey; // 서버가 S3에 올린 사진 key(파일 경로)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductCategory category = ProductCategory.ETC;

    @Column
    private String productName;

    @Column(columnDefinition = "TEXT")
    private String ocrText; // OCR 원문

    @Column(nullable = false)
    @Builder.Default
    private int ingredientCount = 0; // 매칭된 성분 갯수

    // LLM 분석 결과 (분석 완료 시 채워짐)
    @Enumerated(EnumType.STRING)
    @Column
    private RecommendStatus recommended; // LLM 제외/유지 제안 (KEEP/REMOVE)

    @Column(columnDefinition = "TEXT")
    private String recommendReason; // LLM 이유 문구

    // OCR 성분 매칭 결과 갯수 반영
    public void updateIngredientCount(int ingredientCount) {
        this.ingredientCount = ingredientCount;
    }

    // LLM 분석 결과를 제품에 반영하고 Analysis에 매핑
    public void applyAnalysis(Analysis analysis, String productName,
                              RecommendStatus recommended, String recommendReason) {
        this.analysis = analysis;
        this.productName = productName;
        this.recommended = recommended;
        this.recommendReason = recommendReason;
    }
}