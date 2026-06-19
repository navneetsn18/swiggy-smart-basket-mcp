package com.smartbasket.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "substitutions")
public class Substitution {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "preferred_product", nullable = false)
    private String preferredProduct;

    @Column(name = "fallback_product", nullable = false)
    private String fallbackProduct;

    @Column(nullable = false)
    private int priority;

    protected Substitution() {
    }

    public Substitution(String userId, String preferredProduct, String fallbackProduct, int priority) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.preferredProduct = preferredProduct;
        this.fallbackProduct = fallbackProduct;
        this.priority = priority;
    }

    public String getFallbackProduct() {
        return fallbackProduct;
    }

    public int getPriority() {
        return priority;
    }
}
