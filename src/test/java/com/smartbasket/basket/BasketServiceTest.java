package com.smartbasket.basket;

import com.smartbasket.domain.Basket;
import com.smartbasket.domain.BasketRepository;
import com.smartbasket.swiggy.SwiggyGateway;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class BasketServiceTest {

    private final BasketRepository repo = mock(BasketRepository.class);
    private final SwiggyGateway swiggy = mock(SwiggyGateway.class);
    private final BasketService service = new BasketService(repo, swiggy);

    {
        when(repo.save(any(Basket.class))).thenAnswer(i -> i.getArgument(0));
    }

    @Test
    void create_rejects_duplicate() {
        when(repo.existsByUserIdAndName("u1", "Dairy")).thenReturn(true);
        assertThatThrownBy(() -> service.create("u1", "Dairy"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void create_rejects_blank_name() {
        assertThatThrownBy(() -> service.create("u1", "  "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_add_merges_same_product() {
        Basket basket = new Basket("u1", "Dairy");
        basket.addProduct("Milk 1L", 1);
        when(repo.findByUserIdAndName("u1", "Dairy")).thenReturn(Optional.of(basket));

        service.update("u1", "Dairy", "add", "Milk 1L", 2);

        assertThat(basket.getItems()).hasSize(1);
        assertThat(basket.getItems().get(0).getQuantity()).isEqualTo(3);
    }

    @Test
    void update_remove_missing_throws() {
        Basket basket = new Basket("u1", "Dairy");
        when(repo.findByUserIdAndName("u1", "Dairy")).thenReturn(Optional.of(basket));
        assertThatThrownBy(() -> service.update("u1", "Dairy", "remove", "Eggs", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void save_cart_copies_mock_cart_lines() {
        when(repo.existsByUserIdAndName(anyString(), anyString())).thenReturn(false);
        when(swiggy.getCart("u1")).thenReturn(List.of(
                new SwiggyGateway.CartLine("Milk 1L", 2),
                new SwiggyGateway.CartLine("Eggs", 1)));

        Basket result = service.saveCartAsBasket("u1", "From Cart");

        assertThat(result.getItems()).hasSize(2);
        assertThat(result.getItems()).extracting("quantity").containsExactly(2, 1);
    }
}
