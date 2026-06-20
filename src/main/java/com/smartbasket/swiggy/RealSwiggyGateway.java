package com.smartbasket.swiggy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calls the real Swiggy Instamart MCP (mcp.swiggy.com/im) via the mcp-remote bridge.
 *
 * <p>Field names match the live contract captured 2026-06-20:
 * search_products → structuredContent.products[].variations[].{spinId,
 * quantityDescription, displayName, isInStockAndAvailable, price.offerPrice};
 * update_cart needs {selectedAddressId, items:[{spinId, quantity}]}.
 *
 * <p>All shapes here are live-confirmed (2026-06-20): search_products variations and
 * get_cart items ({@code itemName, quantity, spinId}). update_cart's input contract is
 * schema-confirmed but the write was not executed live (it replaces the whole cart).
 */
@Component
@Profile("live")
public class RealSwiggyGateway implements SwiggyGateway {

    private final McpSyncClient mcp;
    private final ObjectMapper json;
    private final String addressId;

    public RealSwiggyGateway(List<McpSyncClient> clients, ObjectMapper json,
                             @Value("${swiggy.address-id:}") String addressId) {
        if (clients.isEmpty()) {
            throw new IllegalStateException("No Swiggy MCP client configured (profile 'live')");
        }
        this.mcp = clients.get(0);
        this.json = json;
        this.addressId = addressId;
    }

    @Override
    public List<Variant> searchVariants(String userId, String query) {
        requireAddress();
        JsonNode root = call("search_products", Map.of("addressId", addressId, "query", query));
        List<Variant> variants = new ArrayList<>();
        for (JsonNode product : root.path("products")) {
            for (JsonNode v : product.path("variations")) {
                variants.add(new Variant(
                        v.path("spinId").asText(),
                        v.path("displayName").asText(product.path("displayName").asText()),
                        v.path("quantityDescription").asText(""),
                        v.path("isInStockAndAvailable").asBoolean(false),
                        v.path("price").path("offerPrice").asInt(0)));
            }
        }
        return variants;
    }

    @Override
    public List<CartLine> getCart(String userId) {
        JsonNode root = call("get_cart", address());
        List<CartLine> lines = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            // Live-confirmed cart item fields: itemName, quantity, spinId.
            lines.add(new CartLine(item.path("itemName").asText(), item.path("quantity").asInt(1)));
        }
        return lines;
    }

    @Override
    public void updateCart(String userId, List<CartItem> items) {
        requireAddress();
        List<Map<String, Object>> payload = items.stream()
                .map(i -> Map.<String, Object>of("spinId", i.spinId(), "quantity", i.quantity()))
                .toList();
        call("update_cart", Map.of("selectedAddressId", addressId, "items", payload));
    }

    private Map<String, Object> address() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (StringUtils.hasText(addressId)) {
            m.put("selectedAddressId", addressId);
        }
        return m;
    }

    private void requireAddress() {
        if (!StringUtils.hasText(addressId)) {
            throw new IllegalStateException("SWIGGY_ADDRESS_ID not set (from get_addresses)");
        }
    }

    /** Call a Swiggy tool and return its structuredContent as a JsonNode. */
    private JsonNode call(String tool, Map<String, Object> args) {
        CallToolResult result = mcp.callTool(new CallToolRequest(tool, args));
        if (Boolean.TRUE.equals(result.isError())) {
            throw new IllegalStateException("Swiggy " + tool + " failed: " + result.content());
        }
        Object structured = result.structuredContent();
        if (structured == null) {
            throw new IllegalStateException("Swiggy " + tool + " returned no structuredContent");
        }
        return json.valueToTree(structured);
    }
}
