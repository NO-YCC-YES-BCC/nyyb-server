package com.nyyb.nyybserver.product.data.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 제품 추가 등록 요청 본문. 검색해도 안 나온 제품을 사용자가 적어 보낸다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductRequestDto {

    // 검색창에 넣었던 말을 그대로 보낸다. (예: "라운드랩 독도 크림")
    private String keyword;
}
