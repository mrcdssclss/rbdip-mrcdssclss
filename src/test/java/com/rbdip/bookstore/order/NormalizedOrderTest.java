package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class NormalizedOrderTest extends AbstractIntegrationTest {

    @Test
    void reusesCustomerWithNullPhoneAndKeepsProductReference() {
        var product = restTemplate.postForEntity("/products", Map.of("name", "Book", "price", "12.50"), Map.class);
        Long productId = Long.valueOf(product.getBody().get("id").toString());
        var request = new CreateOrderRequest("Same Customer", "Address", null, null, null,
                List.of(new CreateOrderRequest.Item(productId, 2)));

        var first = restTemplate.postForEntity("/orders", request, Map.class);
        var second = restTemplate.postForEntity("/orders", request, Map.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(jdbcTemplate.queryForObject("SELECT count(DISTINCT customer_id) FROM orders", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForList("SELECT product_id FROM order_items", Long.class))
                .containsExactly(productId, productId);
        var response = restTemplate.getForEntity("/orders", List.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        Map<?, ?> order = (Map<?, ?>) response.getBody().getFirst();
        assertThat(order.get("customerFullName")).isEqualTo("Same Customer");
        assertThat((List<?>) order.get("items")).hasSize(1);
    }
}
