package com.nyyb.nyybserver.user.data.dto.response;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoProfileResponseDto {

    private Long id;
    private Properties properties;

    public String getNickname() {
        if (properties == null || properties.nickname == null || properties.nickname.isBlank()) {
            return "Kakao User";
        }
        return properties.nickname;
    }

    @Getter
    @NoArgsConstructor
    public static class Properties {
        private String nickname;
    }
}
