package com.nyyb.nyybserver.routine.data.repository;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.routine.data.entity.RoutineItemSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RoutineItemSelectionRepository extends JpaRepository<RoutineItemSelection, Long> {

    // 루틴 전체에서 유저가 제거(REMOVE)로 선택한 슬롯 레코드 수 (BOTH 양쪽 제거 시 자동 2)
    @Query("select count(s) from RoutineItemSelection s " +
            "where s.routineItem.routine.id = :routineId and s.action = :action")
    long countByRoutineIdAndAction(@Param("routineId") UUID routineId,
                                   @Param("action") RecommendStatus action);
}
