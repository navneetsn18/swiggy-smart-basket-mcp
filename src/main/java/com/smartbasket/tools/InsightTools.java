package com.smartbasket.tools;

import com.smartbasket.insights.PurchaseInsightService;
import com.smartbasket.insights.PurchaseInsightService.Insight;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/** MCP tools for AI baskets (Phase 4) and refill prediction (Phase 5). */
@Component
public class InsightTools {

    private final PurchaseInsightService insights;

    public InsightTools(PurchaseInsightService insights) {
        this.insights = insights;
    }

    public record SuggestedItem(String productName, int quantity) {
    }

    public record RefillItem(String productName, long daysSinceLast, int avgGapDays) {
    }

    @Tool(description = "Generate a suggested grocery basket from order history, ranked by how often "
            + "and how recently items are bought (rule-based, no LLM). Returns product names with a "
            + "typical quantity; the user confirms before anything is added.")
    public List<SuggestedItem> generate_ai_basket(
            @ToolParam(description = "User id") String userId,
            @ToolParam(description = "How many items to suggest (default 10)") Integer size) {
        return insights.aiBasket(userId, size == null ? 10 : size).stream()
                .map(i -> new SuggestedItem(i.product(), i.typicalQty()))
                .toList();
    }

    @Tool(description = "Suggest products the user is likely running low on: bought repeatedly and now "
            + "past their usual purchase gap. Returns product, days since last purchase, and the "
            + "average gap.")
    public List<RefillItem> suggest_refill(
            @ToolParam(description = "User id") String userId) {
        return insights.refillDue(userId).stream()
                .map(i -> new RefillItem(i.product(), i.daysSinceLast(), (int) Math.round(i.avgGapDays())))
                .toList();
    }
}
