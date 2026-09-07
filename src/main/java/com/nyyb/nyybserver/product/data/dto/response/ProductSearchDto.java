package com.nyyb.nyybserver.product.data.dto.response;

import com.nyyb.nyybserver.product.data.entity.Product;
import com.nyyb.nyybserver.product.data.enums.CategoryMain;
import com.nyyb.nyybserver.product.data.enums.CategorySub;

public record ProductSearchDto(
        Long productId,      // COSMETIC_REPORT_SEQ (화장품보고일련번호)
        String itemName,     // 품목명
        String entpName,     // 업소명
        String categoryMain, // 카테고리 대분류 (한글 라벨, 미분류면 null)
        String categorySub   // 카테고리 소분류 (한글 라벨, 미분류면 null)
) {
    public static ProductSearchDto from(Product product) {
        return new ProductSearchDto(
                product.getCosmeticReportSeq(),
                product.getItemName(),
                product.getEntpName(),
                label(product.getCategoryMain()),
                label(product.getCategorySub())
        );
    }

    private static String label(CategoryMain category) {
        return category == null ? null : category.getDbValue();
    }

    private static String label(CategorySub category) {
        return category == null ? null : category.getDbValue();
    }
}
