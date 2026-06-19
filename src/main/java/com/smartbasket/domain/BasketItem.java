package com.smartbasket.domain;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "basket_items")
public class BasketItem {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "basket_id", nullable = false)
    private Basket basket;

    // ponytail: product_ref == exact full Swiggy product name (spec Q4); product_name kept identical for now.
    @Column(name = "product_ref", nullable = false)
    private String productRef;

    @Column(name = "product_name", nullable = false)
    private String productName;

    @Column(nullable = false)
    private int quantity;

    protected BasketItem() {
    }

    public BasketItem(Basket basket, String productName, int quantity) {
        this.id = UUID.randomUUID().toString();
        this.basket = basket;
        this.productRef = productName;
        this.productName = productName;
        this.quantity = quantity;
    }

    public String getProductName() {
        return productName;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }
}
