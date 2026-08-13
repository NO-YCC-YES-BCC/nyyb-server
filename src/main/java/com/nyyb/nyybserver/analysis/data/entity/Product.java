package com.nyyb.nyybserver.analysis.data.entity;

import com.nyyb.nyybserver.analysis.data.enums.ProductCategory;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineItemStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import com.nyyb.nyybserver.routine.data.entity.Routine;
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

    // ── [1. 분석] 분석 실행 시 매핑 (스캔~분석 전, 미분석/수집 데이터는 null) ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private Analysis analysis;

    @Column(nullable = false)
    private String imageKey; // 서버가 S3에 올린 사진 key(파일 경로)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ProductCategory category = ProductCategory.ETC;

    @Column(columnDefinition = "TEXT")
    private String ocrText; // OCR 원문

    // ── [3. 제안 / 4. 설정] 루틴 단계에서 채워짐 (저장 전 null) ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id")
    private Routine routine; // 담긴 루틴 (저장 전 null)

    @Enumerated(EnumType.STRING)
    @Column
    private RoutineSlot slot; // 아침/저녁/전체

    @Enumerated(EnumType.STRING)
    @Column
    private RecommendStatus recommended; // LLM 추천 (KEEP/REMOVE)

    @Column(columnDefinition = "TEXT")
    private String recommendReason; // LLM 이유 문구 (카드 본문)

    @Enumerated(EnumType.STRING)
    @Column
    private RoutineItemStatus status; // 유저 선택 (KEPT/REMOVED)
}