package com.nyyb.nyybserver.product.data.dto.response;

import com.nyyb.nyybserver.product.data.entity.Product;

public record ProductSearchDto(
        Long productId,      // 제품 마스터 id
        String name,         // 제품명 (한글 제품명 우선)
        String brand,        // 브랜드명
        String categoryMain, // 카테고리 대분류 (한글 라벨)
        String categorySub   // 카테고리 소분류 (한글 라벨)
) {
    public static ProductSearchDto from(Product product) {
        return new ProductSearchDto(
                product.getId(),
                product.getDisplayName(),
                product.getBrand(),
                product.getCategoryMainLabel(),
                product.getCategorySubLabel()
        );
    }
}
