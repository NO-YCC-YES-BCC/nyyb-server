package com.nyyb.nyybserver.product.data.repository;

import com.nyyb.nyybserver.product.data.entity.ProductRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRequestRepository extends JpaRepository<ProductRequest, Long> {
}
