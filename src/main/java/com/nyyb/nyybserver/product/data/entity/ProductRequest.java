package com.nyyb.nyybserver.product.data.entity;

import com.nyyb.nyybserver.user.data.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 제품 추가 등록 요청.
 * 검색해도 안 나오는 제품을 사용자가 알려 오는 창구다. 요청을 쌓아만 두고,
 * 수집 대상에 넣을지는 사람이 목록을 보고 판단한다.
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "product_request")
@EntityListeners(AuditingEntityListener.class)
public class ProductRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 요청한 사람. 게스트도 요청할 수 있어 탈퇴·병합 시 끊길 수 있으므로 nullable 로 둔다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // 사용자가 검색창에 넣었던 말. 어떤 제품을 찾다 실패했는지가 그대로 남는다.
    @Column(name = "keyword", nullable = false, length = 200)
    private String keyword;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}
