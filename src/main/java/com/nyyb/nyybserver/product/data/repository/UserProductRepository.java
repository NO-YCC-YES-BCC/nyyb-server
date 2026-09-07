package com.nyyb.nyybserver.product.data.repository;

import com.nyyb.nyybserver.analysis.data.enums.RecommendStatus;
import com.nyyb.nyybserver.product.data.entity.UserProduct;
import com.nyyb.nyybserver.user.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProductRepository extends JpaRepository<UserProduct, UUID> {

    // 상세 조회: 해당 분석에 속한 제품들 (등록 순). 마스터 제품을 함께 가져온다.
    @Query("select up from UserProduct up join fetch up.product " +
            "where up.analysis.id = :analysisId order by up.id asc")
    List<UserProduct> findByAnalysisIdWithProduct(@Param("analysisId") UUID analysisId);

    // 목록 조회: 분석별 제품 수 + KEEP/REMOVE 제안 수 집계 (분석 목록 응답용, N+1 방지)
    @Query("select up.analysis.id as analysisId, up.recommended as recommended, count(up) as count " +
            "from UserProduct up where up.analysis.id in :analysisIds group by up.analysis.id, up.recommended")
    List<RecommendCount> countGroupByAnalysisIdAndRecommended(@Param("analysisIds") Collection<UUID> analysisIds);

    interface RecommendCount {
        UUID getAnalysisId();
        RecommendStatus getRecommended();
        long getCount();
    }

    // 소유권 검증 조회 (남의 제품 재매핑 방지)
    @Query("select up from UserProduct up join fetch up.product " +
            "where up.id in :ids and up.user.id = :userId")
    List<UserProduct> findByIdInAndUserIdWithProduct(@Param("ids") Collection<UUID> ids,
                                                     @Param("userId") Long userId);

    @Query("select up from UserProduct up join fetch up.product " +
            "where up.id = :id and up.user.id = :userId")
    Optional<UserProduct> findByIdAndUserIdWithProduct(@Param("id") UUID id, @Param("userId") Long userId);

    // 게스트→소셜 병합 시 소유자 재지정
    @Modifying
    @Query("update UserProduct up set up.user = :newOwner where up.user = :previousOwner")
    int transferOwner(@Param("previousOwner") User previousOwner, @Param("newOwner") User newOwner);
}
