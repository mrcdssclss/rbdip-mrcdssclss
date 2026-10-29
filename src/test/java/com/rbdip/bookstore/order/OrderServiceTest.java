package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(OutputCaptureExtension.class)
class OrderServiceTest {

    private final ProductRepository products = mock(ProductRepository.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final OrderItemRepository items = mock(OrderItemRepository.class);
    private final CustomerRepository customers = mock(CustomerRepository.class);
    private final OrderService service = new OrderService(products, orders, items, customers);

    @BeforeEach
    void prepareCustomer() {
        when(customers.findByFullNameAndAddressAndPhone("Ivan", "Moscow", "123"))
                .thenReturn(Optional.of(new Customer("Ivan", "Moscow", "123")));
    }

    @Test
    void savesItemsInOrderAndPrintsDiscountedTotal(CapturedOutput output) {
        when(products.findById(1L)).thenReturn(Optional.of(product("First", "10.00")));
        when(products.findById(2L)).thenReturn(Optional.of(product("Second", "100.00")));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            ReflectionTestUtils.setField(order, "id", 42L);
            assertThat(output.getOut()).doesNotContain("[email]");
            return order;
        });
        CreateOrderRequest request = request(List.of(
                new CreateOrderRequest.Item(1L, null), new CreateOrderRequest.Item(2L, 11)));

        Order order = service.createOrder(request);

        InOrder sequence = inOrder(products, orders, items);
        sequence.verify(products).findById(1L);
        sequence.verify(products).findById(2L);
        sequence.verify(orders).save(order);
        ArgumentCaptor<OrderItem> savedItems = ArgumentCaptor.forClass(OrderItem.class);
        sequence.verify(items, times(2)).save(savedItems.capture());
        assertThat(savedItems.getAllValues()).extracting(OrderItem::getQuantity).containsExactly(1, 11);
        assertThat(savedItems.getAllValues()).extracting(OrderItem::getProductName).containsExactly("First", "Second");
        assertThat(savedItems.getAllValues()).extracting(OrderItem::getProductPrice)
                .containsExactly(new BigDecimal("10.00"), new BigDecimal("100.00"));
        assertThat(savedItems.getAllValues()).extracting(OrderItem::getOrderId).containsOnly(42L);
        assertThat(order.getStatus()).isEqualTo("new");
        assertThat(order.getCustomerFullName()).isEqualTo("Ivan");
        assertThat(order.getCustomerAddress()).isEqualTo("Moscow");
        assertThat(order.getCustomerPhone()).isEqualTo("123");
        assertThat(output.getOut()).isEqualTo(
                "[email] Dear Ivan, your order #42 for 939.50 has been placed." + System.lineSeparator());
    }

    @Test
    void checksNameBeforeAddressAndItems() {
        CreateOrderRequest request = new CreateOrderRequest(" ", null, null, null, null, null);

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("customerFullName is required");
        verifyNoInteractions(products, orders, items);
    }

    @Test
    void looksUpProductBeforeCheckingQuantity() {
        when(products.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(request(List.of(new CreateOrderRequest.Item(1L, 0)))))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("product 1 not found");
        verifyNoInteractions(orders, items);
    }

    @Test
    void rejectsInvalidLaterItemBeforeSaving(CapturedOutput output) {
        when(products.findById(1L)).thenReturn(Optional.of(product("First", "10.00")));
        when(products.findById(2L)).thenReturn(Optional.of(product("Second", "100.00")));
        CreateOrderRequest request = request(List.of(
                new CreateOrderRequest.Item(1L, 1), new CreateOrderRequest.Item(2L, 0)));

        assertThatThrownBy(() -> service.createOrder(request))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("quantity must be positive");
        verifyNoInteractions(orders, items);
        assertThat(output.getOut()).doesNotContain("[email]");
    }

    @Test
    void doesNotSendEmailWhenSavingItemFails(CapturedOutput output) {
        when(products.findById(1L)).thenReturn(Optional.of(product("First", "10.00")));
        when(orders.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        IllegalStateException failure = new IllegalStateException("save failed");
        when(items.save(any(OrderItem.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.createOrder(request(List.of(new CreateOrderRequest.Item(1L, 1)))))
                .isSameAs(failure);
        assertThat(output.getOut()).doesNotContain("[email]");
    }

    private CreateOrderRequest request(List<CreateOrderRequest.Item> orderItems) {
        return new CreateOrderRequest("Ivan", "Moscow", "123", "vip", "SAVE10", orderItems);
    }

    private Product product(String name, String price) {
        return new Product(name, new BigDecimal(price), null);
    }
}
