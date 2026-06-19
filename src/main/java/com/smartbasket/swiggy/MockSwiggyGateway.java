package com.smartbasket.swiggy;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Stand-in for the Swiggy Instamart MCP until access is granted (spec Q2).
 * Returns a fixed sample cart so basket features are fully testable now.
 */
@Component
public class MockSwiggyGateway implements SwiggyGateway {

    @Override
    public List<CartLine> getCart(String userId) {
        return List.of(
                new CartLine("Amul Full Cream Milk 1L", 2),
                new CartLine("Farm Eggs 10 pcs", 1),
                new CartLine("Amul Masti Curd 400g", 1)
        );
    }
}
