package com.nyyb.nyybserver.routine.data.repository;

import com.nyyb.nyybserver.routine.data.entity.Routine;
import com.nyyb.nyybserver.user.data.entity.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoutineRepository extends JpaRepository<Routine, UUID> {

    // 상세 조회: 소유자까지 조건에 넣어 남의 루틴은 조회되지 않도록
    Optional<Routine> findByIdAndUserId(UUID id, Long userId);

    // 분석 상세 조회 시 함께 내려줄 루틴 id (분석 1 : 루틴 1, 유니크 제약이 없어 가장 먼저 만들어진 것 기준)
    Optional<Routine> findFirstByAnalysisIdOrderByCreatedAtAsc(UUID analysisId);

    // 분석 목록에 루틴 점수를 붙이려고 여러 분석의 점수를 한 번에 가져온다. (N+1 방지)
    // 분석 1 : 루틴 1 이지만 유니크 제약이 없어, 호출부에서 가장 먼저 만들어진 루틴 것만 쓰도록 생성순으로 정렬해 둔다.
    @Query("select r.analysis.id as analysisId, r.score as score from Routine r "
            + "where r.analysis.id in :analysisIds order by r.createdAt asc")
    List<AnalysisScore> findScoresByAnalysisIds(@Param("analysisIds") List<UUID> analysisIds);

    interface AnalysisScore {
        UUID getAnalysisId();

        Integer getScore(); // 루틴 설계 전이면 null
    }

    // 목록 조회: 현재 로그인 유저(게스트/카카오 공통)의 루틴을 최신순으로
    // 정렬 파라미터를 받지 않으므로 생성일 → id 순으로 고정 정렬 (페이지 간 순서 보장)
    List<Routine> findByUserIdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    // 마이페이지 통계 기준이 되는 가장 최신 루틴 (목록과 동일한 정렬 기준)
    Optional<Routine> findFirstByUserIdOrderByCreatedAtDescIdDesc(Long userId);

    // 게스트→소셜 병합 시 소유자 재지정
    @Modifying
    @Query("update Routine r set r.user = :newOwner where r.user = :previousOwner")
    int transferOwner(@Param("previousOwner") User previousOwner, @Param("newOwner") User newOwner);
}
