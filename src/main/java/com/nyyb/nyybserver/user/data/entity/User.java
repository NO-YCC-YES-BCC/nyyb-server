package com.nyyb.nyybserver.user.data.entity;

import com.nyyb.nyybserver.user.data.enums.AuthProvider;
import com.nyyb.nyybserver.user.data.enums.UserRole;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

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

    @Column(unique = true)
    private Long kakaoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    private AuthProvider provider;

    @Column(name = "provider_id", nullable = false)
    private String providerId;

    @Column(nullable = false)
    private String nickname;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, columnDefinition = "varchar(20)")
    @Builder.Default
    private UserRole role = UserRole.USER;

    @Column(name = "merged_to_user_id")
    private Long mergedToUserId;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column
    private LocalDateTime modifiedAt;

    public static User ofSocial(AuthProvider provider, Long kakaoId, String providerId, String nickname) {
        return User.builder()
                .kakaoId(kakaoId)
                .provider(provider)
                .providerId(providerId)
                .nickname(nickname)
                .role(UserRole.USER)
                .build();
    }

    public static User ofGuest() {
        String guestId = UUID.randomUUID().toString();
        return User.builder()
                .provider(AuthProvider.GUEST)
                .providerId(guestId)
                .nickname("Guest")
                .role(UserRole.GUEST)
                .build();
    }

    public boolean isGuest() {
        return provider == AuthProvider.GUEST;
    }

    public boolean isMergedGuest() {
        return mergedToUserId != null;
    }

    public void connectSocial(AuthProvider provider, Long kakaoId, String providerId, String nickname) {
        this.provider = provider;
        this.kakaoId = kakaoId;
        this.providerId = providerId;
        this.nickname = nickname;
        this.role = UserRole.USER;
        this.mergedToUserId = null;
    }

    public void syncProfile(String nickname) {
        this.nickname = nickname;
    }

    public void mergeTo(User targetUser) {
        this.mergedToUserId = targetUser.getId();
    }

}
