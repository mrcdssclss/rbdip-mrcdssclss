package com.rbdip.bookstore.order;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public class PricingCalculator {

    private static final int BULK_QUANTITY_THRESHOLD = 10;
    private static final BigDecimal BULK_PRICE_MULTIPLIER = BigDecimal.valueOf(0.95);
    private static final BigDecimal VIP_PRICE_MULTIPLIER = BigDecimal.valueOf(0.9);
    private static final BigDecimal WHOLESALE_PRICE_MULTIPLIER = BigDecimal.valueOf(0.85);
    private static final BigDecimal COUPON_PRICE_MULTIPLIER = BigDecimal.valueOf(0.8);
    private static final BigDecimal LARGE_ORDER_THRESHOLD = BigDecimal.valueOf(1000);
    private static final BigDecimal LARGE_ORDER_PRICE_MULTIPLIER = BigDecimal.valueOf(0.98);

    public record LineItem(BigDecimal price, int quantity) {
    }

    public BigDecimal calculateOrderTotal(List<LineItem> items, String customerType, String couponCode) {
        BigDecimal total = BigDecimal.ZERO;

        for (LineItem item : items) {
            BigDecimal linePrice = item.price().multiply(BigDecimal.valueOf(item.quantity()));
            if (item.quantity() > BULK_QUANTITY_THRESHOLD) {
                linePrice = linePrice.multiply(BULK_PRICE_MULTIPLIER);
            }
            total = total.add(linePrice);
        }

        total = applyCustomerDiscount(total, customerType);
        total = applyCoupon(total, couponCode);

        if (total.compareTo(BigDecimal.ZERO) < 0) {
            total = BigDecimal.ZERO;
        }

        if (total.compareTo(LARGE_ORDER_THRESHOLD) > 0) {
            total = total.multiply(LARGE_ORDER_PRICE_MULTIPLIER);
        }

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal applyCustomerDiscount(BigDecimal total, String customerType) {
        if ("vip".equals(customerType)) {
            return total.multiply(VIP_PRICE_MULTIPLIER);
        }
        if ("wholesale".equals(customerType)) {
            return total.multiply(WHOLESALE_PRICE_MULTIPLIER);
        }

        return total;
    }

    private BigDecimal applyCoupon(BigDecimal total, String couponCode) {
        if ("SAVE10".equals(couponCode)) {
            return total.subtract(BigDecimal.TEN);
        }
        if ("SAVE20PERCENT".equals(couponCode)) {
            return total.multiply(COUPON_PRICE_MULTIPLIER);
        }

        return total;
    }
}
