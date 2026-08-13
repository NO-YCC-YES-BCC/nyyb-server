package com.nyyb.nyybserver.routine.data.repository;

import com.nyyb.nyybserver.routine.data.entity.RoutineItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RoutineItemRepository extends JpaRepository<RoutineItem, Long> {

    // 제품 fetch join (createRoutine에서 product 접근)
    @Query("select ri from RoutineItem ri " +
            "join fetch ri.product " +
            "where ri.routine.id = :routineId")
    List<RoutineItem> findByRoutineIdWithProduct(@Param("routineId") UUID routineId);
}
