package com.smartbasket.swiggy;

import java.time.Instant;
import java.util.List;

/**
 * Boundary to the Swiggy Instamart MCP. The Swiggy cart is keyed by {@code spinId}
 * (a specific product variant). The delivery address is an implementation detail
 * (default address, spec Q3) so it is not part of this contract.
 */
public interface SwiggyGateway {

    /** A purchasable product variant returned by search. */
    record Variant(String spinId, String displayName, String quantityDescription,
                   boolean inStock, int offerPrice) {
    }

    /** A line in the current cart (for display). */
    record CartLine(String productName, int quantity) {
    }

    /** A line to put in the cart (Swiggy update_cart replaces the whole cart). */
    record CartItem(String spinId, int quantity) {
    }

    /** A past-order line item. */
    record OrderItem(String name, int quantity) {
    }

    /** A past order. */
    record Order(Instant placedAt, List<OrderItem> items) {
    }

    /** Variants matching a query at the user's default address (in-stock and not). */
    List<Variant> searchVariants(String userId, String query);

    List<CartLine> getCart(String userId);

    void updateCart(String userId, List<CartItem> items);

    /** Past orders, newest first, for frequency/recency analysis. */
    List<Order> getOrders(String userId);
}
