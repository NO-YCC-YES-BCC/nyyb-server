package com.nyyb.nyybserver.routine.data.repository;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.routine.data.entity.RoutineItemSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RoutineItemSelectionRepository extends JpaRepository<RoutineItemSelection, Long> {

    // 루틴 전체에서 유저가 해당 action(KEEP/REMOVE)으로 선택한 슬롯 레코드 수 (BOTH 양쪽 선택 시 자동 2)
    @Query("select count(s) from RoutineItemSelection s " +
            "where s.routineItem.routine.id = :routineId and s.action = :action")
    long countByRoutineIdAndAction(@Param("routineId") UUID routineId,
                                   @Param("action") RecommendStatus action);

    // 해당 action으로 선택된 제품(루틴 아이템) 수 — 슬롯 레코드가 아닌 제품 단위.
    // BOTH 제품이 양쪽 슬롯에서 선택돼도 1로 센다.
    @Query("select count(distinct s.routineItem.id) from RoutineItemSelection s " +
            "where s.routineItem.routine.id = :routineId and s.action = :action")
    long countDistinctItemsByRoutineIdAndAction(@Param("routineId") UUID routineId,
                                                @Param("action") RecommendStatus action);
}
