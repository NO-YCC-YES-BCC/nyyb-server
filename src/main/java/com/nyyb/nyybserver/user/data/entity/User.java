package com.nyyb.nyybserver.user.data.entity;

import com.nyyb.nyybserver.user.data.enums.Role;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

@Entity
@Getter
@NoArgsConstructor
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String provider;

    private String providerId;

    private String email;

    private String nickname;

    @Enumerated(EnumType.STRING)
    private Role role;

    @Builder
    public User(String provider, String providerId){
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }
}
