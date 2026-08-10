package com.nyyb.nyybserver.user.data.dto.response;

import com.nyyb.nyybserver.common.security.AuthTokens;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SocialLoginResponseDto {
    private Long id;
    private String nickname;
    private boolean guest;
    private Long linkedGuestUserId;
    private AuthTokens token;

    public SocialLoginResponseDto(Long id, String nickname, AuthTokens token) {
        this(id, nickname, false, null, token);
    }
    public SocialLoginResponseDto(Long id, String nickname, boolean guest, Long linkedGuestUserId, AuthTokens token) {
        this.id = id;
        this.nickname = nickname;
        this.guest = guest;
        this.linkedGuestUserId = linkedGuestUserId;
        this.token = token;
    }
}
