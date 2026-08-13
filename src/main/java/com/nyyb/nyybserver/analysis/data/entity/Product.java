package com.nyyb.nyybserver.analysis.data.entity;

import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
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

    // LLM 분석 결과 (분석 완료 시 채워짐)
    @Enumerated(EnumType.STRING)
    @Column
    private RecommendStatus recommended; // LLM 제외/유지 제안 (KEEP/REMOVE)

    @Column(columnDefinition = "TEXT")
    private String recommendReason; // LLM 이유 문구

    // LLM 분석 결과를 제품에 반영하고 Analysis에 매핑
    public void applyAnalysis(Analysis analysis, String productName,
                              RecommendStatus recommended, String recommendReason) {
        this.analysis = analysis;
        this.productName = productName;
        this.recommended = recommended;
        this.recommendReason = recommendReason;
    }
}