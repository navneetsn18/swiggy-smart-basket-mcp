package com.smartbasket.basket;

import com.smartbasket.domain.Basket;
import com.smartbasket.domain.BasketItem;
import com.smartbasket.substitution.SubstitutionService;
import com.smartbasket.swiggy.SwiggyGateway;
import com.smartbasket.swiggy.SwiggyGateway.CartItem;
import com.smartbasket.swiggy.SwiggyGateway.Variant;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Plans how a basket maps onto Swiggy products (LLM-mediated, spec issue #2 option c):
 * resolves unambiguous in-stock items to a spinId, applies learned substitutions when
 * the preferred item is out of stock, and surfaces ambiguous items (multiple variants)
 * for the user to choose. It does NOT write the cart — the AI client collects the user's
 * choices and calls {@code update_instamart_cart}. Never checks out.
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

    /** An item resolved to a single product variant. {@code substituteFor} is the preferred name when substituted, else null. */
    public record Resolved(String productName, String spinId, int quantity, String substituteFor) {
    }

    /** An item with several in-stock variants — the user must pick one. */
    public record Choice(String productName, int quantity, List<Variant> variants) {
    }

    public record Plan(List<Resolved> resolved, List<Choice> needChoice, List<String> unavailable) {
    }

    public Plan planBasket(String userId, String basketName) {
        Basket basket = baskets.get(userId, basketName);
        List<Resolved> resolved = new ArrayList<>();
        List<Choice> needChoice = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();

        for (BasketItem item : basket.getItems()) {
            String preferred = item.getProductName();
            int qty = item.getQuantity();
            List<Variant> inStock = inStock(preferred);

            if (inStock.size() == 1) {
                resolved.add(new Resolved(preferred, inStock.get(0).spinId(), qty, null));
            } else if (inStock.size() > 1) {
                needChoice.add(new Choice(preferred, qty, inStock));
            } else {
                Resolved sub = firstInStockFallback(userId, preferred, qty);
                if (sub != null) {
                    resolved.add(sub);
                } else {
                    unavailable.add(preferred);
                }
            }
        }
        return new Plan(resolved, needChoice, unavailable);
    }

    /** Commit the chosen variants to the cart. Replaces the whole cart (Swiggy semantics). */
    public void commit(String userId, List<CartItem> items) {
        swiggy.updateCart(userId, items);
    }

    private List<Variant> inStock(String query) {
        return swiggy.searchVariants(null, query).stream().filter(Variant::inStock).toList();
    }

    private Resolved firstInStockFallback(String userId, String preferred, int qty) {
        for (String fallback : substitutions.chain(userId, preferred)) {
            List<Variant> hits = inStock(fallback);
            if (!hits.isEmpty()) {
                return new Resolved(fallback, hits.get(0).spinId(), qty, preferred);
            }
        }
        return null;
    }
}
