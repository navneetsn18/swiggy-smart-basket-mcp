package com.smartbasket.basket;

import com.smartbasket.domain.Basket;
import com.smartbasket.substitution.SubstitutionService;
import com.smartbasket.swiggy.SwiggyGateway;
import com.smartbasket.swiggy.SwiggyGateway.CartLine;
import com.smartbasket.swiggy.SwiggyGateway.ProductHit;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class FulfillmentServiceTest {

    private final BasketService baskets = mock(BasketService.class);
    private final SubstitutionService subs = mock(SubstitutionService.class);
    private final SwiggyGateway swiggy = mock(SwiggyGateway.class);
    private final FulfillmentService service = new FulfillmentService(baskets, subs, swiggy);

    @Test
    void substitutes_when_preferred_out_of_stock() {
        Basket basket = new Basket("u1", "Dairy");
        basket.addProduct("Amul Milk 1L", 2);
        when(baskets.get("u1", "Dairy")).thenReturn(basket);

        when(swiggy.searchProduct("u1", "Amul Milk 1L")).thenReturn(Optional.of(new ProductHit("Amul Milk 1L", false)));
        when(subs.chain("u1", "Amul Milk 1L")).thenReturn(List.of("Amul Milk 500ml"));
        when(swiggy.searchProduct("u1", "Amul Milk 500ml")).thenReturn(Optional.of(new ProductHit("Amul Milk 500ml", true)));

        FulfillmentService.Summary summary = service.addBasketToCart("u1", "Dairy");

        assertThat(summary.substitutions()).containsExactly(new FulfillmentService.Applied("Amul Milk 1L", "Amul Milk 500ml"));
        assertThat(summary.added()).containsExactly(new CartLine("Amul Milk 500ml", 2));
        assertThat(summary.unavailable()).isEmpty();
        verify(swiggy).updateCart(eq("u1"), anyList());
    }

    @Test
    void reports_unavailable_when_no_fallback_in_stock() {
        Basket basket = new Basket("u1", "Dairy");
        basket.addProduct("Rare Cheese", 1);
        when(baskets.get("u1", "Dairy")).thenReturn(basket);
        when(swiggy.searchProduct("u1", "Rare Cheese")).thenReturn(Optional.of(new ProductHit("Rare Cheese", false)));
        when(subs.chain("u1", "Rare Cheese")).thenReturn(List.of());

        FulfillmentService.Summary summary = service.addBasketToCart("u1", "Dairy");

        assertThat(summary.unavailable()).containsExactly("Rare Cheese");
        assertThat(summary.added()).isEmpty();
    }
}
