package com.smartbasket.insights;

import com.smartbasket.swiggy.SwiggyGateway;
import com.smartbasket.swiggy.SwiggyGateway.Order;
import com.smartbasket.swiggy.SwiggyGateway.OrderItem;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rule-based grocery intelligence (idea.md "AI Logic v1" — no LLM). Reads order
 * history and derives per-product frequency, recency, typical quantity, and average
 * purchase gap. Feeds AI baskets (Phase 4) and refill prediction (Phase 5).
 *
 * <p>ponytail: computed on demand from get_orders; the purchase_insights table is left
 * for later if caching is ever needed.
 */
@Service
public class PurchaseInsightService {

    private final SwiggyGateway swiggy;

    public PurchaseInsightService(SwiggyGateway swiggy) {
        this.swiggy = swiggy;
    }

    public record Insight(String product, int orderCount, int typicalQty,
                          long daysSinceLast, double avgGapDays) {
    }

    /** One insight per product seen in order history. */
    public List<Insight> analyze(String userId) {
        Map<String, List<Instant>> dates = new LinkedHashMap<>();
        Map<String, List<Integer>> qtys = new LinkedHashMap<>();
        for (Order order : swiggy.getOrders(userId)) {
            for (OrderItem item : order.items()) {
                dates.computeIfAbsent(item.name(), k -> new ArrayList<>()).add(order.placedAt());
                qtys.computeIfAbsent(item.name(), k -> new ArrayList<>()).add(item.quantity());
            }
        }
        Instant now = Instant.now();
        List<Insight> insights = new ArrayList<>();
        for (String product : dates.keySet()) {
            List<Instant> when = dates.get(product).stream().sorted().toList();
            Instant last = when.get(when.size() - 1);
            insights.add(new Insight(product, when.size(), mode(qtys.get(product)),
                    Duration.between(last, now).toDays(), avgGapDays(when)));
        }
        return insights;
    }

    /** Suggested basket: most-frequent items first, then most overdue. */
    public List<Insight> aiBasket(String userId, int size) {
        return analyze(userId).stream()
                .sorted(Comparator.comparingInt(Insight::orderCount).reversed()
                        .thenComparing(Comparator.comparingLong(Insight::daysSinceLast).reversed()))
                .limit(Math.max(1, size))
                .toList();
    }

    /** Products likely running low: bought repeatedly and now past their usual gap. */
    public List<Insight> refillDue(String userId) {
        return analyze(userId).stream()
                .filter(i -> i.orderCount() >= 2 && i.avgGapDays() > 0 && i.daysSinceLast() >= i.avgGapDays())
                .sorted(Comparator.comparingDouble((Insight i) -> i.daysSinceLast() - i.avgGapDays()).reversed())
                .toList();
    }

    /** Average days between consecutive purchases; 0 if fewer than two. */
    private static double avgGapDays(List<Instant> sortedAsc) {
        if (sortedAsc.size() < 2) {
            return 0;
        }
        double totalDays = 0;
        for (int i = 1; i < sortedAsc.size(); i++) {
            totalDays += Duration.between(sortedAsc.get(i - 1), sortedAsc.get(i)).toHours() / 24.0;
        }
        return totalDays / (sortedAsc.size() - 1);
    }

    /** Most common quantity (ties → larger). */
    private static int mode(List<Integer> values) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        values.forEach(v -> counts.merge(v, 1, Integer::sum));
        return counts.entrySet().stream()
                .max(Comparator.<Map.Entry<Integer, Integer>>comparingInt(Map.Entry::getValue)
                        .thenComparingInt(Map.Entry::getKey))
                .map(Map.Entry::getKey).orElse(1);
    }
}
