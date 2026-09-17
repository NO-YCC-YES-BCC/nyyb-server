package com.nyyb.nyybserver.product.controller;

import com.nyyb.nyybserver.product.data.dto.request.ProductRequestDto;
import com.nyyb.nyybserver.product.data.dto.response.ProductSearchDto;
import com.nyyb.nyybserver.product.data.dto.response.ProductSuggestionDto;
import com.nyyb.nyybserver.product.service.ProductRequestService;
import com.nyyb.nyybserver.product.service.ProductService;
import com.nyyb.nyybserver.common.dto.PageRequestDto;
import com.nyyb.nyybserver.common.security.SecurityUtil;
import com.nyyb.nyybserver.common.response.GlobalResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final ProductRequestService productRequestService;

    // 제품명 검색. 공백으로 구분한 여러 단어를 모두 포함하는 제품을 찾는다. (예: keyword=브랜드명 크림)
    @GetMapping
    public GlobalResponse<List<ProductSearchDto>> searchProducts(
            @RequestParam String keyword,
            @ParameterObject PageRequestDto pageRequest
    ) {
        return GlobalResponse.ok(productSearchService.search(keyword, pageRequest.toPageable()));
    }

    // 검색창 자동완성. 검색과 같은 조건으로 찾되 목록에 필요한 id·이름만 내려준다.
    // 입력 중에 반복 호출되는 자리라 페이징 없이 상위 몇 건만 돌려주고, limit 은 최대 20으로 잘린다.
    @Operation(summary = "제품명 자동완성", description = "검색창 입력 중 보여줄 제품명 후보를 가져온다.")
    @GetMapping("/suggestions")
    public GlobalResponse<List<ProductSuggestionDto>> suggestProducts(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return GlobalResponse.ok(productSearchService.suggest(keyword, limit));
    }

    // 검색해도 안 나오는 제품을 사용자가 알려 오는 창구. 요청을 쌓아만 두고 수집 대상 반영은 사람이 판단한다.
    @Operation(summary = "제품 추가 등록 요청", description = "검색 결과가 없을 때 사용자가 제품 등록을 요청한다.")
    @PostMapping("/requests")
    public GlobalResponse<Void> requestProduct(@RequestBody ProductRequestDto request) {
        productRequestService.register(request, SecurityUtil.getUserId());
        return GlobalResponse.ok();
    }
}
