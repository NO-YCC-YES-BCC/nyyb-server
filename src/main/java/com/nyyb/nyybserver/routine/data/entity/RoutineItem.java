package com.nyyb.nyybserver.routine.data.entity;

import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.analysis.data.enums.RoutineSlot;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

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
    private RecommendStatus recommended; // LLM 추천 (KEEP/REMOVE) — 슬롯은 llmRoutineSlot에 별도 저장

    @Column(columnDefinition = "TEXT")
    private String recommendReason; // LLM 이유 문구 (카드 본문)

    // 유저의 슬롯별 선택 (KEEP/REMOVE). BOTH 제품은 최대 2개(MORNING/EVENING).
    @Builder.Default
    @OneToMany(mappedBy = "routineItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoutineItemSelection> selections = new ArrayList<>();

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // designRoutine 단계에서 LLM 루틴 설계 결과(슬롯/추천/이유)를 반영
    public void applyLlmRoutine(RoutineSlot llmRoutineSlot, RecommendStatus recommended, String recommendReason) {
        this.llmRoutineSlot = llmRoutineSlot;
        this.recommended = recommended;
        this.recommendReason = recommendReason;
    }

    /**
     * saveRoutine 단계에서 유저의 슬롯별 선택(KEEP/REMOVE)을 upsert.
     * 같은 slot 레코드가 있으면 action만 갱신, 없으면 새로 추가한다.
     * 오전/저녁 화면이 독립적으로 저장돼도 서로 다른 slot 레코드라 병합 로직이 필요 없다.
     */
    public void applyUserSelection(RoutineSlot slot, RecommendStatus action) {
        for (RoutineItemSelection selection : selections) {
            if (selection.getSlot() == slot) {
                selection.changeAction(action);
                return;
            }
        }
        selections.add(RoutineItemSelection.builder()
                .routineItem(this)
                .slot(slot)
                .action(action)
                .build());
    }
}
