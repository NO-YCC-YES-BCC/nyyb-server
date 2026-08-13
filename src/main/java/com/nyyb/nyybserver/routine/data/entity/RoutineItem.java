package com.nyyb.nyybserver.routine.data.entity;

import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineItemStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "routine_item")
@EntityListeners(AuditingEntityListener.class)
public class RoutineItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 루틴 1 : 아이템 N
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "routine_id", nullable = false)
    private Routine routine;

    // 제품 1 : 루틴아이템 0..1 — 한 제품은 최대 하나의 루틴 아이템으로만 담김
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", unique = true, nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column
    private RoutineSlot userRoutineSlot; // 유저가 고른 슬롯 (아침/저녁/전체)

    @Enumerated(EnumType.STRING)
    @Column
    private RoutineSlot llmRoutineSlot; // LLM 추천 슬롯 (아침/저녁/전체)

    @Enumerated(EnumType.STRING)
    @Column
    private RecommendStatus recommended; // LLM 추천 (KEEP/REMOVE)

    @Column(columnDefinition = "TEXT")
    private String recommendReason; // LLM 이유 문구 (카드 본문)

    @Enumerated(EnumType.STRING)
    @Column
    private RoutineItemStatus status; // 유저 선택 (KEPT/REMOVED)

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
