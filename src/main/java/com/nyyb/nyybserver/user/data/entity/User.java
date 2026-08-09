package com.nyyb.nyybserver.user.data.entity;

import com.nyyb.nyybserver.user.data.enums.AuthProvider;
import com.nyyb.nyybserver.user.data.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(
        name = "user",
        uniqueConstraints = {
                // provider + provider_id 조합은 중복 불가 (같은 소셜 계정 재가입 방지)
                @UniqueConstraint(name = "uk_user_provider", columnNames = {"provider", "provider_id"})
        }
)
@EntityListeners(AuditingEntityListener.class)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column
    private String email;

    @Column(nullable = false)
    private String name;

    @Column
    private String profileImageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(nullable = false)
    @Builder.Default
    private Boolean notifyKakao = false;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column
    private LocalDateTime modifiedAt;

    public static User ofSocial(AuthProvider provider, String providerId, String email,
                                String name, String profileImageUrl) {
        return User.builder()
                .provider(provider)
                .providerId(providerId)
                .email(email)
                .name(name)
                .profileImageUrl(profileImageUrl)
                .role(UserRole.USER)
                .build();
    }

    public void syncProfile(String name, String profileImageUrl) {
        if (name != null) {
            this.name = name;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
    }
}