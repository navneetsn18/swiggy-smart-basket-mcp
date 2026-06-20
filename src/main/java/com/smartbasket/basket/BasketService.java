package com.smartbasket.basket;

import com.smartbasket.domain.Basket;
import com.smartbasket.domain.BasketRepository;
import com.smartbasket.swiggy.SwiggyGateway;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@Transactional
public class BasketService {

    private final BasketRepository repo;
    private final SwiggyGateway swiggy;

    public BasketService(BasketRepository repo, SwiggyGateway swiggy) {
        this.repo = repo;
        this.swiggy = swiggy;
    }

    public Basket create(String userId, String name) {
        requireText(userId, "userId");
        requireText(name, "basketName");
        if (repo.existsByUserIdAndName(userId, name)) {
            throw new IllegalArgumentException("Basket already exists: " + name);
        }
        return repo.save(new Basket(userId, name));
    }

    @Transactional(readOnly = true)
    public Basket get(String userId, String name) {
        requireText(userId, "userId");
        requireText(name, "basketName");
        return repo.findByUserIdAndName(userId, name)
                .orElseThrow(() -> new IllegalArgumentException("No such basket: " + name));
    }

    @Transactional(readOnly = true)
    public List<String> listNames(String userId) {
        requireText(userId, "userId");
        return repo.findByUserId(userId).stream().map(Basket::getName).toList();
    }

    public Basket update(String userId, String basketName, String action, String productName, int quantity) {
        Basket basket = get(userId, basketName);
        requireText(productName, "productName");
        switch (action == null ? "" : action.toLowerCase()) {
            case "add" -> {
                if (quantity < 1) throw new IllegalArgumentException("quantity must be >= 1 to add");
                basket.addProduct(productName, quantity);
            }
            case "remove" -> {
                if (!basket.removeProduct(productName)) {
                    throw new IllegalArgumentException("Product not in basket: " + productName);
                }
            }
            default -> throw new IllegalArgumentException("action must be 'add' or 'remove'");
        }
        return repo.save(basket);
    }

    public void delete(String userId, String name) {
        repo.delete(get(userId, name));
    }

    /** Snapshot the user's current Instamart cart into a new basket. */
    public Basket saveCartAsBasket(String userId, String name) {
        Basket basket = create(userId, name);
        for (SwiggyGateway.CartLine line : swiggy.getCart(userId)) {
            basket.addProduct(line.productName(), line.quantity());
        }
        return repo.save(basket);
    }

    private static void requireText(String value, String field) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }
}
