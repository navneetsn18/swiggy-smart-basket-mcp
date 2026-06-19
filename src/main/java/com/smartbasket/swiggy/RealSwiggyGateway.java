package com.smartbasket.swiggy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.spec.McpSchema.CallToolRequest;
import io.modelcontextprotocol.spec.McpSchema.CallToolResult;
import io.modelcontextprotocol.spec.McpSchema.TextContent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Calls the real Swiggy Instamart MCP (POST mcp.swiggy.com/im) via mcp-remote.
 *
 * <p>⚠️ KNOWN-INCOMPLETE — blocked, see GitHub issues. A live smoke test (2026-06-20)
 * captured the real contract, which differs from this code:
 * <ul>
 *   <li>The cart is keyed by <b>spinId</b> (product variant id from search), not name.
 *       {@code update_cart} needs {@code selectedAddressId} + {@code items:[{spinId,quantity}]}.</li>
 *   <li>{@code search_products} requires an <b>addressId</b> (from {@code get_addresses}).</li>
 *   <li>The machine-readable data is in the response's {@code structuredContent}
 *       (e.g. {@code products[].variations[].spinId / isInStockAndAvailable}). But MCP
 *       Java SDK 0.10.0's {@code CallToolResult} exposes only {@code content()} (prose
 *       text) — it drops {@code structuredContent}. So this gateway cannot read the
 *       clean JSON without an SDK/Spring AI upgrade.</li>
 * </ul>
 * The parsing below is therefore a placeholder. Do not enable {@code live} for real
 * orders until the integration approach is decided.
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
    public List<CartLine> getCart(String userId) {
        JsonNode root = call("get_cart", Map.of());
        List<CartLine> lines = new ArrayList<>();
        for (JsonNode item : root.path("items")) {
            lines.add(new CartLine(text(item, "name", "product_name"), item.path("quantity").asInt(1)));
        }
        return lines;
    }

    @Override
    public Optional<ProductHit> searchProduct(String userId, String query) {
        Map<String, Object> args = new LinkedHashMap<>();
        args.put("query", query);
        if (StringUtils.hasText(addressId)) {
            args.put("addressId", addressId);
        }
        JsonNode products = call("search_products", args).path("products");
        if (!products.isArray() || products.isEmpty()) {
            return Optional.empty();
        }
        JsonNode top = products.get(0);
        boolean available = top.path("available").asBoolean(top.path("inStock").asBoolean(true));
        return Optional.of(new ProductHit(text(top, "name", "product_name"), available));
    }

    @Override
    public void updateCart(String userId, List<CartLine> lines) {
        List<Map<String, Object>> items = lines.stream()
                .map(l -> Map.<String, Object>of("name", l.productName(), "quantity", l.quantity()))
                .toList();
        call("update_cart", Map.of("items", items));
    }

    private JsonNode call(String tool, Map<String, Object> args) {
        CallToolResult result = mcp.callTool(new CallToolRequest(tool, args));
        if (Boolean.TRUE.equals(result.isError())) {
            throw new IllegalStateException("Swiggy tool " + tool + " failed: " + result.content());
        }
        String body = result.content().stream()
                .filter(c -> c instanceof TextContent)
                .map(c -> ((TextContent) c).text())
                .findFirst()
                .orElse("{}");
        try {
            return json.readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("Unparseable Swiggy response for " + tool + ": " + body, e);
        }
    }

    private static String text(JsonNode node, String... fields) {
        for (String f : fields) {
            if (node.hasNonNull(f)) {
                return node.get(f).asText();
            }
        }
        return "";
    }
}
