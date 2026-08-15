package com.nyyb.nyybserver.routine.data.repository;

import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.user.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface RoutineRepository extends JpaRepository<Routine, UUID> {

    // 게스트→소셜 병합 시 소유자 재지정
    @Modifying
    @Query("update Routine r set r.user = :newOwner where r.user = :previousOwner")
    int transferOwner(@Param("previousOwner") User previousOwner, @Param("newOwner") User newOwner);
}
