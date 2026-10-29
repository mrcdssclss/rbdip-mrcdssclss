package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class NormalizationMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void preservesLegacyDataAndEnforcesReferences() {
        var source = new DriverManagerDataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
        JdbcTemplate jdbc = new JdbcTemplate(source);
        Flyway.configure().dataSource(source).target("1").load().migrate();
        jdbc.execute("""
                INSERT INTO orders (customer_full_name, customer_address, customer_phone) VALUES
                ('Ivan', 'A', '1'), ('Ivan', 'A', '1'), ('Ivan', 'B', '1'),
                ('Ivan', 'A', '2'), ('Ivan', NULL, NULL), ('Ivan', NULL, NULL), ('Ivan', '', '');
                INSERT INTO products (name, price, description) VALUES
                ('Book', 10, 'first'), ('Book', 10, 'duplicate'), ('Book', 20, 'other price');
                INSERT INTO order_items (order_id, product_name, product_price, quantity) VALUES
                (1, 'Book', 10, 2), (2, 'Book', 20, 1), (3, 'Missing', 30, 3), (4, 'Missing', 30, 0);
                """);
        Flyway.configure().dataSource(source).target("2").load().migrate();
        jdbc.execute("INSERT INTO orders (customer_full_name) VALUES ('Late')");
        Flyway.configure().dataSource(source).load().migrate();

        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isEqualTo(8);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM customers", Integer.class)).isEqualTo(6);
        assertThat(jdbc.queryForList("SELECT customer_id FROM orders ORDER BY id", Long.class))
                .satisfies(ids -> {
                    assertThat(ids.get(0)).isEqualTo(ids.get(1));
                    assertThat(ids.get(4)).isEqualTo(ids.get(5));
                    assertThat(ids.get(0)).isNotEqualTo(ids.get(2)).isNotEqualTo(ids.get(3));
                    assertThat(ids.get(4)).isNotEqualTo(ids.get(6));
                });
        assertThat(jdbc.queryForList("SELECT product_id FROM order_items ORDER BY id", Long.class))
                .containsExactly(1L, 3L, 4L, 4L);
        assertThat(jdbc.queryForList("SELECT quantity FROM order_items ORDER BY id", Integer.class))
                .containsExactly(2, 1, 3, 0);
        assertThat(jdbc.queryForObject("SELECT sum(p.price * i.quantity) FROM order_items i JOIN products p ON p.id = i.product_id",
                java.math.BigDecimal.class)).isEqualByComparingTo("130");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM products", Integer.class)).isEqualTo(4);
        assertThatThrownBy(() -> jdbc.execute("UPDATE orders SET customer_id = NULL WHERE id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.execute("UPDATE orders SET customer_id = -1 WHERE id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.execute("UPDATE order_items SET product_id = -1 WHERE id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.execute("INSERT INTO customers (full_name) VALUES ('Ivan')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
