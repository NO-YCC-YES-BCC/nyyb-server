package com.nyyb.nyybserver.product.data.converter;

import com.nyyb.nyybserver.product.data.enums.CategoryMain;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * CATEGORY_MAIN 컬럼(한글 라벨 enum) ↔ {@link CategoryMain} 상수 변환.
 * 상수 이름(영문)이 아니라 DB에 실제로 저장된 한글 대분류 라벨을 읽고 쓴다.
 */
@Converter
public class CategoryMainConverter implements AttributeConverter<CategoryMain, String> {

    @Override
    public String convertToDatabaseColumn(CategoryMain attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public CategoryMain convertToEntityAttribute(String dbData) {
        return CategoryMain.from(dbData);
    }
}
