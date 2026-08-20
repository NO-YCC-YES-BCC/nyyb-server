package com.nyyb.nyybserver.ingredient.data.dto.response;

import com.nyyb.nyybserver.ingredient.data.entity.Allergic;

public record AllergicDto(
        Long id,
        String name,
        String dataSource,
        String description
) {
    // 알레르기 유발 물질로 매칭된 성분에 공통으로 내려주는 고정 안내 문구
    private static final String DESCRIPTION = "식품의약품안전처 자료에 근거해 알레르기 유발성분으로 지정된 성분이에요.";

    public static AllergicDto from(Allergic allergic) {
        return new AllergicDto(
                allergic.getId(),
                allergic.getName(),
                allergic.getDataSource(),
                DESCRIPTION
        );
    }
}
