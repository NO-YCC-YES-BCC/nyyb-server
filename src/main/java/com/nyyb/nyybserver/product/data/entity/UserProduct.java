package com.nyyb.nyybserver.product.data.entity;

import com.nyyb.nyybserver.analysis.data.entity.Analysis;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.user.data.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * 사용자가 자기 것으로 담은 제품 1건.
 * 제품 자체의 속성(제품명·브랜드·카테고리·전성분)은 마스터 {@link Product}가 갖고,
 * 여기에는 "누구의 것인지 / 어느 분석에 속하는지 / LLM이 무엇을 제안했는지"만 둔다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_product")
public class UserProduct {

    // 외부(응답·루틴)에 노출되는 식별자라 순차 증가 대신 UUID로 만든다. (남의 제품 id 열거 방지)
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    private UUID id;

    // 소유자(게스트/카카오 공통). 게스트→소셜 병합 시 재지정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 제품 마스터 카탈로그 참조
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private Analysis analysis;

    // LLM 분석 결과 (분석 완료 시 채워짐)
    @Enumerated(EnumType.STRING)
    @Column
    private RecommendStatus recommended; // LLM 제외/유지 제안 (KEEP/REMOVE)

    @Column(columnDefinition = "TEXT")
    private String recommendReason; // LLM 이유 문구

    // LLM 분석 결과를 반영하고 Analysis에 매핑
    public void applyAnalysis(Analysis analysis, RecommendStatus recommended, String recommendReason) {
        this.analysis = analysis;
        this.recommended = recommended;
        this.recommendReason = recommendReason;
    }
}
