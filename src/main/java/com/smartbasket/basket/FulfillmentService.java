package com.smartbasket.basket;

import com.smartbasket.domain.Basket;
import com.smartbasket.domain.BasketItem;
import com.smartbasket.substitution.SubstitutionService;
import com.smartbasket.swiggy.SwiggyGateway;
import com.smartbasket.swiggy.SwiggyGateway.CartLine;
import com.smartbasket.swiggy.SwiggyGateway.ProductHit;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Adds a saved basket to the user's Swiggy cart, applying learned substitutions
 * when the preferred product is unavailable. Never checks out — that's the user's
 * call (idea.md core philosophy).
 */
@Service
public class FulfillmentService {

    private final BasketService baskets;
    private final SubstitutionService substitutions;
    private final SwiggyGateway swiggy;

    public FulfillmentService(BasketService baskets, SubstitutionService substitutions, SwiggyGateway swiggy) {
        this.baskets = baskets;
        this.substitutions = substitutions;
        this.swiggy = swiggy;
    }

    public record Applied(String preferred, String usedInstead) {
    }

    public record Summary(List<CartLine> added, List<Applied> substitutions, List<String> unavailable) {
    }

    public Summary addBasketToCart(String userId, String basketName) {
        Basket basket = baskets.get(userId, basketName);

        List<CartLine> added = new ArrayList<>();
        List<Applied> applied = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();

        for (BasketItem item : basket.getItems()) {
            String preferred = item.getProductName();
            int qty = item.getQuantity();

            if (isAvailable(userId, preferred)) {
                added.add(new CartLine(preferred, qty));
                continue;
            }
            // Preferred out of stock — walk the learned fallback chain.
            String substitute = firstAvailableFallback(userId, preferred);
            if (substitute != null) {
                added.add(new CartLine(substitute, qty));
                applied.add(new Applied(preferred, substitute));
            } else {
                unavailable.add(preferred);
            }
        }

        swiggy.updateCart(userId, added);
        return new Summary(added, applied, unavailable);
    }

    private boolean isAvailable(String userId, String productName) {
        return swiggy.searchProduct(userId, productName).map(ProductHit::available).orElse(false);
    }

    private String firstAvailableFallback(String userId, String preferred) {
        for (String fallback : substitutions.chain(userId, preferred)) {
            Optional<ProductHit> hit = swiggy.searchProduct(userId, fallback);
            if (hit.isPresent() && hit.get().available()) {
                return fallback;
            }
        }
        return null;
    }
}
