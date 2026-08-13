package com.nyyb.nyybserver.routine.data.repository;

import com.nyyb.nyybserver.routine.data.entity.Routine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface RoutineRepository extends JpaRepository<Routine, UUID> {
}
