package com.smartbasket.swiggy;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Stand-in for the Swiggy Instamart MCP (default profile). Search rules, for tests:
 * <ul>
 *   <li>query contains "1L"  → out of stock (exercises substitution),</li>
 *   <li>query contains "Bread" → two in-stock variants (exercises user choice),</li>
 *   <li>otherwise → one in-stock variant.</li>
 * </ul>
 */
@Component
@Profile("!live")
public class MockSwiggyGateway implements SwiggyGateway {

    private final List<CartItem> cart = new ArrayList<>();

    @Override
    public List<Variant> searchVariants(String userId, String query) {
        if (query.contains("1L")) {
            return List.of(new Variant(spin(query), query, "1 L", false, 70));
        }
        if (query.contains("Bread")) {
            return List.of(
                    new Variant(spin(query + "-400"), query, "400 g", true, 45),
                    new Variant(spin(query + "-800"), query, "800 g", true, 80));
        }
        return List.of(new Variant(spin(query), query, "1 unit", true, 50));
    }

    @Override
    public List<CartLine> getCart(String userId) {
        return cart.stream().map(i -> new CartLine(i.spinId(), i.quantity())).toList();
    }

    @Override
    public void updateCart(String userId, List<CartItem> items) {
        cart.clear();
        cart.addAll(items);
    }

    private static String spin(String s) {
        return "MOCK-" + Integer.toHexString(s.hashCode());
    }
}
