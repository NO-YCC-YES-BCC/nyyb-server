package com.nyyb.nyybserver.ingredient.data.entity;

import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ingredient")
public class Ingredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 예: Vitamin C

    @Column(nullable = false)
    @Builder.Default
    private Boolean isToxic = false;

    @Enumerated(EnumType.STRING)
    @Column
    private RiskLevel riskLevel;

    @Column(columnDefinition = "TEXT")
    private String description; // 성분 가이드 설명
}
