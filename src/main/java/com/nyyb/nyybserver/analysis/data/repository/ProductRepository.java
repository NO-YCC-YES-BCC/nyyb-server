package com.nyyb.nyybserver.analysis.data.repository;

import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.user.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 상세 조회: 해당 분석에 속한 제품들 (등록 순)
    List<Product> findByAnalysisIdOrderByIdAsc(UUID analysisId);

    // 목록 조회: 분석별 제품 수 + KEEP/REMOVE 제안 수 집계 (분석 목록 응답용, N+1 방지)
    @Query("select p.analysis.id as analysisId, p.recommended as recommended, count(p) as count " +
            "from Product p where p.analysis.id in :analysisIds group by p.analysis.id, p.recommended")
    List<RecommendCount> countGroupByAnalysisIdAndRecommended(@Param("analysisIds") Collection<UUID> analysisIds);

    interface RecommendCount {
        UUID getAnalysisId();
        RecommendStatus getRecommended();
        long getCount();
    }

    // analyze 요청 검증: 요청한 productId 중 본인 소유인 것만 (남의 제품 재매핑 방지)
    List<Product> findByIdInAndUserId(Collection<Long> ids, Long userId);

    // 단일 제품 소유권 검증 조회
    Optional<Product> findByIdAndUserId(Long id, Long userId);

    // 게스트→소셜 병합 시 소유자 재지정
    @Modifying
    @Query("update Product p set p.user = :newOwner where p.user = :previousOwner")
    int transferOwner(@Param("previousOwner") User previousOwner, @Param("newOwner") User newOwner);
}
