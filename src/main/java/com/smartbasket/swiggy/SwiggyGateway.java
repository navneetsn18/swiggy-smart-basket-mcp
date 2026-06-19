package com.smartbasket.swiggy;

import java.util.List;
import java.util.Optional;

/**
 * Boundary to the Swiggy Instamart MCP. Two implementations:
 * {@code MockSwiggyGateway} (default) and {@code RealSwiggyGateway} (profile "live").
 */
public interface SwiggyGateway {

    /** A line in the user's Instamart cart. */
    record CartLine(String productName, int quantity) {
    }

    /** A product match from search. */
    record ProductHit(String productName, boolean available) {
    }

    /** Current cart contents. */
    List<CartLine> getCart(String userId);

    /** Top search match for a query, if any. */
    Optional<ProductHit> searchProduct(String userId, String query);

    /** Replace the entire cart with these lines (Swiggy update_cart replaces, not merges). */
    void updateCart(String userId, List<CartLine> lines);
}
