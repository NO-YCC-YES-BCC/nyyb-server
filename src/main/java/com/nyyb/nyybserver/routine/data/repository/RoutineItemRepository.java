package com.nyyb.nyybserver.routine.data.repository;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface RoutineItemRepository extends JpaRepository<RoutineItem, Long> {

    // 제품 fetch join (designRoutine에서 product 접근)
    @Query("select ri from RoutineItem ri " +
            "join fetch ri.product " +
            "where ri.routine.id = :routineId")
    List<RoutineItem> findByRoutineIdWithProduct(@Param("routineId") UUID routineId);

    // 루틴에 담긴 제품 수 (루틴아이템 1 : 제품 1)
    // Report detail: load products and the user's slot selections together.
    @Query("select distinct ri from RoutineItem ri " +
            "join fetch ri.product " +
            "left join fetch ri.selections " +
            "where ri.routine.id = :routineId")
    List<RoutineItem> findByRoutineIdWithProductAndSelections(@Param("routineId") UUID routineId);

    long countByRoutineId(UUID routineId);

    // 목록 조회: 루틴별 KEEP/REMOVE 추천 수 집계 (분석 목록과 동일 패턴, N+1 방지)
    @Query("select ri.routine.id as routineId, ri.recommended as recommended, count(ri) as count " +
            "from RoutineItem ri where ri.routine.id in :routineIds group by ri.routine.id, ri.recommended")
    List<RecommendCount> countGroupByRoutineIdAndRecommended(@Param("routineIds") Collection<UUID> routineIds);

    interface RecommendCount {
        UUID getRoutineId();
        RecommendStatus getRecommended();
        long getCount();
    }
}
