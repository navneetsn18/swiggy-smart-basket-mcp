package com.smartbasket.swiggy;

import java.util.List;

/**
 * Boundary to the Swiggy Instamart MCP. Phase 1 only needs the current cart
 * (for save_cart_as_basket). Search / cart-update land in Phase 2 with the real
 * MCP client behind this same interface.
 */
public interface SwiggyGateway {

    /** A line in the user's current Instamart cart. */
    record CartLine(String productName, int quantity) {
    }

    List<CartLine> getCart(String userId);
}
