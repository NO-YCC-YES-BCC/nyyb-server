package com.nyyb.nyybserver.product.controller;

import com.nyyb.nyybserver.product.data.dto.response.ProductSearchDto;
import com.nyyb.nyybserver.product.service.ProductService;
import com.nyyb.nyybserver.common.dto.PageRequestDto;
import com.nyyb.nyybserver.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/products")
@Tag(name = "Product", description = "Product master APIs")
public class ProductController {

    private final ProductService productSearchService;

    // 제품명 검색. 공백으로 구분한 여러 단어를 모두 포함하는 제품을 찾는다. (예: keyword=브랜드명 크림)
    @GetMapping
    public GlobalResponse<List<ProductSearchDto>> searchProducts(
            @RequestParam String keyword,
            @ParameterObject PageRequestDto pageRequest
    ) {
        return GlobalResponse.ok(productSearchService.search(keyword, pageRequest.toPageable()));
    }
}
