package com.nyyb.nyybserver.product.data.repository;

import com.nyyb.nyybserver.product.data.dto.response.ProductSuggestionDto;
import com.nyyb.nyybserver.product.data.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public interface ProductRepositoryCustom {

    /**
     * 자동완성용 조회. 이름 컬럼만 select 해서 가져온다.
     *
     * @param spec  {@link ProductSpecifications} 로 만든 검색 조건
     * @param limit 가져올 최대 건수
     */
    List<ProductSuggestionDto> findSuggestions(Specification<Product> spec, int limit);
}
