package com.nyyb.nyybserver.ingredient.data.dto.response;

import com.nyyb.nyybserver.ingredient.data.entity.Allergic;

public record AllergicDto(
        Long id,
        String name,
        String dataSource
) {
    public static AllergicDto from(Allergic allergic) {
        return new AllergicDto(
                allergic.getId(),
                allergic.getName(),
                allergic.getDataSource()
        );
    }
}
