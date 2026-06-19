package com.smartbasket.swiggy;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Stand-in for the Swiggy Instamart MCP. Default (non-"live" profile) so all
 * basket/substitution features are testable without Swiggy access.
 *
 * <p>To exercise the substitution path, any product whose name contains
 * "1L" is treated as out of stock.
 */
@Component
@Profile("!live")
public class MockSwiggyGateway implements SwiggyGateway {

    private final List<CartLine> cart = new ArrayList<>(List.of(
            new CartLine("Amul Full Cream Milk 1L", 2),
            new CartLine("Farm Eggs 10 pcs", 1),
            new CartLine("Amul Masti Curd 400g", 1)
    ));

    @Override
    public List<CartLine> getCart(String userId) {
        return List.copyOf(cart);
    }

    @Override
    public Optional<ProductHit> searchProduct(String userId, String query) {
        boolean available = !query.contains("1L"); // ponytail: simple OOS rule for tests
        return Optional.of(new ProductHit(query, available));
    }

    @Override
    public void updateCart(String userId, List<CartLine> lines) {
        cart.clear();
        cart.addAll(lines);
    }
}
