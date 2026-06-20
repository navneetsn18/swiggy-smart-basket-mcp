package com.smartbasket.insights;

import com.smartbasket.swiggy.SwiggyGateway;
import com.smartbasket.swiggy.SwiggyGateway.Order;
import com.smartbasket.swiggy.SwiggyGateway.OrderItem;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PurchaseInsightServiceTest {

    private final SwiggyGateway swiggy = mock(SwiggyGateway.class);
    private final PurchaseInsightService service = new PurchaseInsightService(swiggy);

    private void history() {
        Instant now = Instant.now();
        // Milk x3 (~3-day gap, last 4 days ago → due); Eggs x2 (~6-day gap, last 4 days ago → not due); Bread x1.
        when(swiggy.getOrders("u1")).thenReturn(List.of(
                new Order(now.minus(4, ChronoUnit.DAYS), List.of(new OrderItem("Milk", 2), new OrderItem("Eggs", 1))),
                new Order(now.minus(7, ChronoUnit.DAYS), List.of(new OrderItem("Milk", 2), new OrderItem("Bread", 1))),
                new Order(now.minus(10, ChronoUnit.DAYS), List.of(new OrderItem("Milk", 2), new OrderItem("Eggs", 1)))));
    }

    @Test
    void ai_basket_ranks_by_frequency() {
        history();
        List<PurchaseInsightService.Insight> basket = service.aiBasket("u1", 2);
        assertThat(basket).extracting(PurchaseInsightService.Insight::product).containsExactly("Milk", "Eggs");
        assertThat(basket.get(0).typicalQty()).isEqualTo(2);
    }

    @Test
    void refill_due_flags_overdue_repeat_purchases() {
        history();
        List<PurchaseInsightService.Insight> due = service.refillDue("u1");
        // Milk: gap ~3d, last 4d ago → due. Eggs: gap ~6d, last 4d ago → not due. Bread: bought once → excluded.
        assertThat(due).extracting(PurchaseInsightService.Insight::product).containsExactly("Milk");
    }
}
