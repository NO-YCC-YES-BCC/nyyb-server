package com.nyyb.nyybserver.analysis.data.repository;

import com.nyyb.nyybserver.analysis.data.entity.Product;
import com.nyyb.nyybserver.user.data.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // 상세 조회: 해당 분석에 속한 제품들 (등록 순)
    List<Product> findByAnalysisIdOrderByIdAsc(UUID analysisId);

    // analyze 요청 검증: 요청한 productId 중 본인 소유인 것만 (남의 제품 재매핑 방지)
    List<Product> findByIdInAndUserId(Collection<Long> ids, Long userId);

    // 게스트→소셜 병합 시 소유자 재지정
    @Modifying
    @Query("update Product p set p.user = :newOwner where p.user = :previousOwner")
    int transferOwner(@Param("previousOwner") User previousOwner, @Param("newOwner") User newOwner);
}
