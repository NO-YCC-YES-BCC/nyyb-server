package com.nyyb.nyybserver.ingredient.data.repository;

import com.nyyb.nyybserver.ingredient.data.entity.Allergic;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AllergicRepository extends JpaRepository<Allergic, Long> {
}
