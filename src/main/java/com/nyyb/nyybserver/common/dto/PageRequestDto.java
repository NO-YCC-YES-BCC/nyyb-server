package com.nyyb.nyybserver.common.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Getter
@Setter
public class PageRequestDto {

    private int page = 0;

    private int size = 20;

    public Pageable toPageable() {
        int p = Math.max(page, 0);
        int s = size < 1 ? 20 : size;
        return PageRequest.of(p, s);
    }
}
