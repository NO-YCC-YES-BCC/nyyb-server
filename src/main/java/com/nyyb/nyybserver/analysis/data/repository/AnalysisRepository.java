package com.nyyb.nyybserver.analysis.data.repository;

import com.nyyb.nyybserver.analysis.data.entity.Analysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {
}
