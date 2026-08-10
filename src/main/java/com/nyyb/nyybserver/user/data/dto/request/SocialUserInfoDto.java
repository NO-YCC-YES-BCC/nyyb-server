package com.nyyb.nyybserver.user.data.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialUserInfoDto {
    private Long id;
    private String nickname;

    public SocialUserInfoDto(Long id, String nickname, String email) {
        this.id = id;
        this.nickname = nickname;
    }
}
