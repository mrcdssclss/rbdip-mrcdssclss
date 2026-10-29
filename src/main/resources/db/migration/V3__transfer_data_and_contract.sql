LOCK TABLE orders, order_items, products IN ACCESS EXCLUSIVE MODE;

INSERT INTO customers (full_name, address, phone)
SELECT DISTINCT customer_full_name, customer_address, customer_phone FROM orders
ON CONFLICT (full_name, address, phone) DO NOTHING;

UPDATE orders o
SET customer_id = c.id
FROM customers c
WHERE c.full_name = o.customer_full_name
  AND c.address IS NOT DISTINCT FROM o.customer_address
  AND c.phone IS NOT DISTINCT FROM o.customer_phone;

INSERT INTO products (name, price)
SELECT DISTINCT i.product_name, i.product_price
FROM order_items i
WHERE NOT EXISTS (
    SELECT 1 FROM products p WHERE p.name = i.product_name AND p.price = i.product_price
);

UPDATE order_items i
SET product_id = (
    SELECT MIN(p.id) FROM products p
    WHERE p.name = i.product_name AND p.price = i.product_price
);

ALTER TABLE orders
    ALTER COLUMN customer_id SET NOT NULL,
    DROP COLUMN customer_full_name,
    DROP COLUMN customer_address,
    DROP COLUMN customer_phone;

ALTER TABLE order_items
    ALTER COLUMN product_id SET NOT NULL,
    DROP COLUMN product_name,
    DROP COLUMN product_price;
