package com.nyyb.nyybserver.user.data.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class KakaoLoginRequestDto {
    @Schema(description = "Kakao authorization code", example = "authorization-code-from-kakao")
    private String code;
}
