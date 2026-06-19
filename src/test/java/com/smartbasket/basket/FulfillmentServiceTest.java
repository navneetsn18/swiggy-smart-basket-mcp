package com.smartbasket.basket;

import com.smartbasket.domain.Basket;
import com.smartbasket.substitution.SubstitutionService;
import com.smartbasket.swiggy.SwiggyGateway;
import com.smartbasket.swiggy.SwiggyGateway.Variant;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FulfillmentServiceTest {

    private final BasketService baskets = mock(BasketService.class);
    private final SubstitutionService subs = mock(SubstitutionService.class);
    private final SwiggyGateway swiggy = mock(SwiggyGateway.class);
    private final FulfillmentService service = new FulfillmentService(baskets, subs, swiggy);

    private void basketWith(String product, int qty) {
        Basket b = new Basket("u1", "B");
        b.addProduct(product, qty);
        when(baskets.get("u1", "B")).thenReturn(b);
    }

    @Test
    void single_in_stock_variant_resolves() {
        basketWith("Eggs", 1);
        when(swiggy.searchVariants(any(), eq("Eggs")))
                .thenReturn(List.of(new Variant("SPIN_EGG", "Eggs", "6 pcs", true, 60)));

        FulfillmentService.Plan plan = service.planBasket("u1", "B");

        assertThat(plan.resolved()).containsExactly(new FulfillmentService.Resolved("Eggs", "SPIN_EGG", 1, null));
        assertThat(plan.needChoice()).isEmpty();
    }

    @Test
    void multiple_variants_need_user_choice() {
        basketWith("Bread", 1);
        when(swiggy.searchVariants(any(), eq("Bread"))).thenReturn(List.of(
                new Variant("SPIN_400", "Bread", "400 g", true, 45),
                new Variant("SPIN_800", "Bread", "800 g", true, 80)));

        FulfillmentService.Plan plan = service.planBasket("u1", "B");

        assertThat(plan.resolved()).isEmpty();
        assertThat(plan.needChoice()).hasSize(1);
        assertThat(plan.needChoice().get(0).variants()).hasSize(2);
    }

    @Test
    void out_of_stock_uses_substitute() {
        basketWith("Amul Milk 1L", 2);
        when(swiggy.searchVariants(any(), eq("Amul Milk 1L")))
                .thenReturn(List.of(new Variant("SPIN_1L", "Amul Milk 1L", "1 L", false, 70)));
        when(subs.chain("u1", "Amul Milk 1L")).thenReturn(List.of("Amul Milk 500ml"));
        when(swiggy.searchVariants(any(), eq("Amul Milk 500ml")))
                .thenReturn(List.of(new Variant("SPIN_500", "Amul Milk 500ml", "500 ml", true, 35)));

        FulfillmentService.Plan plan = service.planBasket("u1", "B");

        assertThat(plan.resolved())
                .containsExactly(new FulfillmentService.Resolved("Amul Milk 500ml", "SPIN_500", 2, "Amul Milk 1L"));
    }

    @Test
    void no_stock_no_fallback_is_unavailable() {
        basketWith("Rare Cheese", 1);
        when(swiggy.searchVariants(any(), eq("Rare Cheese")))
                .thenReturn(List.of(new Variant("SPIN_X", "Rare Cheese", "200 g", false, 200)));
        when(subs.chain("u1", "Rare Cheese")).thenReturn(List.of());

        FulfillmentService.Plan plan = service.planBasket("u1", "B");

        assertThat(plan.unavailable()).containsExactly("Rare Cheese");
        assertThat(plan.resolved()).isEmpty();
    }
}
