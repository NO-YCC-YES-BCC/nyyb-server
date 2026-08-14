package com.nyyb.nyybserver.user.data.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Getter
@NoArgsConstructor
public class KakaoProfileResponseDto {

    private Long id;
    private Properties properties;

    @JsonProperty("kakao_account")
    private KakaoAccount kakaoAccount;

    public String getNickname() {
        if (kakaoAccount != null
                && kakaoAccount.profile != null
                && StringUtils.hasText(kakaoAccount.profile.nickname)) {
            return kakaoAccount.profile.nickname;
        }
        if (properties != null && StringUtils.hasText(properties.nickname)) {
            return properties.nickname;
        }
        return "Kakao User";
    }

    @Getter
    @NoArgsConstructor
    public static class Properties {
        private String nickname;
    }

    @Getter
    @NoArgsConstructor
    public static class KakaoAccount {
        private String email;
        private Profile profile;
    }

    @Getter
    @NoArgsConstructor
    public static class Profile {
        private String nickname;
    }
}
