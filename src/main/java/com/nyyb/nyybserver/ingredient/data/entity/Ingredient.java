package com.nyyb.nyybserver.ingredient.data.entity;

import com.nyyb.nyybserver.ingredient.data.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    // 이명 - 매칭 시 name과 함께 키워드로 사용
    @OneToMany(mappedBy = "ingredient", fetch = FetchType.LAZY)
    @Builder.Default
    private List<IngredientAlias> aliases = new ArrayList<>();
}
