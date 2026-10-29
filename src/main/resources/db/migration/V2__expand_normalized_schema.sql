CREATE TABLE customers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    address VARCHAR(500),
    phone VARCHAR(50),
    CONSTRAINT customers_contact_unique UNIQUE NULLS NOT DISTINCT (full_name, address, phone)
);

ALTER TABLE orders ADD COLUMN customer_id BIGINT REFERENCES customers(id);
ALTER TABLE order_items ADD COLUMN product_id BIGINT REFERENCES products(id);

CREATE INDEX orders_customer_id_idx ON orders(customer_id);
CREATE INDEX order_items_product_id_idx ON order_items(product_id);
CREATE INDEX order_items_order_id_idx ON order_items(order_id);
