package com.smartbasket.tools;

import com.smartbasket.basket.BasketService;
import com.smartbasket.basket.FulfillmentService;
import com.smartbasket.domain.Basket;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * MCP tools exposed to AI clients. user_id is supplied per request by the client
 * (spec Q1). Phase 1: basket CRUD + save-cart. Substitutions / AI baskets / refill
 * arrive in later phases.
 */
@Component
public class BasketTools {

    private final BasketService baskets;
    private final FulfillmentService fulfillment;

    public BasketTools(BasketService baskets, FulfillmentService fulfillment) {
        this.baskets = baskets;
        this.fulfillment = fulfillment;
    }

    public record ItemView(String productName, int quantity) {
    }

    public record BasketView(String name, List<ItemView> items) {
    }

    @Tool(description = "Create a new empty grocery basket for the user.")
    public BasketView create_basket(
            @ToolParam(description = "User id, supplied by the client") String userId,
            @ToolParam(description = "Basket name, e.g. 'Dairy Basket'") String name) {
        return view(baskets.create(userId, name));
    }

    @Tool(description = "Return a basket's items.")
    public BasketView get_basket(
            @ToolParam(description = "User id") String userId,
            @ToolParam(description = "Basket name") String basketName) {
        return view(baskets.get(userId, basketName));
    }

    @Tool(description = "Add or remove a product in a basket.")
    public BasketView update_basket(
            @ToolParam(description = "User id") String userId,
            @ToolParam(description = "Basket name") String basketName,
            @ToolParam(description = "'add' or 'remove'") String action,
            @ToolParam(description = "Exact full product name") String productName,
            @ToolParam(description = "Quantity (for 'add'); ignored for 'remove'") int quantity) {
        return view(baskets.update(userId, basketName, action, productName, quantity));
    }

    @Tool(description = "Delete a basket.")
    public String delete_basket(
            @ToolParam(description = "User id") String userId,
            @ToolParam(description = "Basket name") String basketName) {
        baskets.delete(userId, basketName);
        return "Deleted basket: " + basketName;
    }

    @Tool(description = "List the names of all baskets for the user.")
    public List<String> list_baskets(
            @ToolParam(description = "User id") String userId) {
        return baskets.listNames(userId);
    }

    @Tool(description = "Snapshot the user's current Swiggy Instamart cart into a new basket.")
    public BasketView save_cart_as_basket(
            @ToolParam(description = "User id") String userId,
            @ToolParam(description = "Name for the new basket") String basketName) {
        return view(baskets.saveCartAsBasket(userId, basketName));
    }

    @Tool(description = "Add a saved basket to the user's Swiggy Instamart cart, applying learned "
            + "substitutions for out-of-stock items. Does NOT checkout. Returns what was added, "
            + "any substitutions applied, and items that could not be fulfilled.")
    public FulfillmentService.Summary add_basket_to_instamart(
            @ToolParam(description = "User id") String userId,
            @ToolParam(description = "Basket name to add") String basketName) {
        return fulfillment.addBasketToCart(userId, basketName);
    }

    private static BasketView view(Basket b) {
        List<ItemView> items = b.getItems().stream()
                .map(i -> new ItemView(i.getProductName(), i.getQuantity()))
                .toList();
        return new BasketView(b.getName(), items);
    }
}
