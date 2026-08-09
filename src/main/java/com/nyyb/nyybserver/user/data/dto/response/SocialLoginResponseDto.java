package com.nyyb.nyybserver.user.data.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class SocialLoginResponseDto {
    private String name;
    private String accessToken;
    private boolean isNewUser;
}
