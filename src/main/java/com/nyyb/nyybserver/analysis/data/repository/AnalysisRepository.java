package com.nyyb.nyybserver.analysis.data.repository;

import com.nyyb.nyybserver.analysis.data.entity.Analysis;
import com.nyyb.nyybserver.user.data.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AnalysisRepository extends JpaRepository<Analysis, UUID> {

    // 상세 조회: 소유자까지 조건에 넣어 남의 분석은 조회되지 않도록
    Optional<Analysis> findByIdAndUserId(UUID id, Long userId);

    // 목록 조회: 현재 로그인 유저(게스트/카카오 공통)의 분석을 최신순으로
    // 정렬 파라미터를 받지 않으므로 생성일 → id 순으로 고정 정렬 (페이지 간 순서 보장)
    List<Analysis> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    // 마이페이지 분석 횟수
    long countByUserId(Long userId);

    // 게스트→소셜 병합 시 소유자 재지정
    @Modifying
    @Query("update Analysis a set a.user = :newOwner where a.user = :previousOwner")
    int transferOwner(@Param("previousOwner") User previousOwner, @Param("newOwner") User newOwner);
}
