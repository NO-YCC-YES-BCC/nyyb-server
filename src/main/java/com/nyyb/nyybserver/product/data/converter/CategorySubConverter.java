package com.nyyb.nyybserver.product.data.converter;

import com.nyyb.nyybserver.product.data.enums.CategorySub;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * CATEGORY_SUB 컬럼(한글 라벨 enum) ↔ {@link CategorySub} 상수 변환.
 * 상수 이름(영문)이 아니라 DB에 실제로 저장된 한글 소분류 라벨을 읽고 쓴다.
 */
@Converter
public class CategorySubConverter implements AttributeConverter<CategorySub, String> {

    @Override
    public String convertToDatabaseColumn(CategorySub attribute) {
        return attribute == null ? null : attribute.getDbValue();
    }

    @Override
    public CategorySub convertToEntityAttribute(String dbData) {
        return CategorySub.from(dbData);
    }
}
