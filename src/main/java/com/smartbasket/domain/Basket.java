package com.smartbasket.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "baskets")
public class Basket {

    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private String name;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "basket", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<BasketItem> items = new ArrayList<>();

    protected Basket() {
    }

    public Basket(String userId, String name) {
        this.id = UUID.randomUUID().toString();
        this.userId = userId;
        this.name = name;
        this.createdAt = Instant.now();
    }

    /** Add qty of a product, merging into an existing line if present. */
    public void addProduct(String productName, int quantity) {
        for (BasketItem item : items) {
            if (item.getProductName().equalsIgnoreCase(productName)) {
                item.setQuantity(item.getQuantity() + quantity);
                return;
            }
        }
        items.add(new BasketItem(this, productName, quantity));
    }

    /** Remove a product line entirely. Returns true if something was removed. */
    public boolean removeProduct(String productName) {
        return items.removeIf(i -> i.getProductName().equalsIgnoreCase(productName));
    }

    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<BasketItem> getItems() {
        return items;
    }
}
