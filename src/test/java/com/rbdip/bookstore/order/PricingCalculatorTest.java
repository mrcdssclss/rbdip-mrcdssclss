package com.rbdip.bookstore.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PricingCalculatorTest {

    private final PricingCalculator calculator = new PricingCalculator();

    @ParameterizedTest
    @CsvSource({
        "10, 0, regular, , 0.00",
        "10, 1, regular, , 10.00",
        "10, 10, regular, , 100.00",
        "10, 11, regular, , 104.50",
        "100, 1, vip, , 90.00",
        "100, 1, wholesale, , 85.00",
        "100, 1, regular, SAVE10, 90.00",
        "100, 1, regular, SAVE20PERCENT, 80.00",
        "100, 1, vip, SAVE10, 80.00",
        "100, 1, wholesale, SAVE10, 75.00",
        "100, 1, vip, SAVE20PERCENT, 72.00",
        "100, 1, wholesale, SAVE20PERCENT, 68.00",
        "100, 1, unknown, UNKNOWN, 100.00",
        "100, 1, VIP, save10, 100.00",
        "100, 1, , , 100.00",
        "100, 1, '', '', 100.00",
        "0, 11, vip, SAVE10, 0.00",
        "5, 1, regular, SAVE10, 0.00",
        "10, 1, regular, SAVE10, 0.00",
        "-10, 1, regular, , 0.00",
        "10, -1, regular, , 0.00",
        "-10, -2, regular, , 20.00",
        "-10, 11, wholesale, SAVE20PERCENT, 0.00",
        "999.99, 1, regular, , 999.99",
        "1000, 1, regular, , 1000.00",
        "1000.01, 1, regular, , 980.01",
        "1010, 1, regular, SAVE10, 1000.00",
        "1010.01, 1, regular, SAVE10, 980.01",
        "1100, 1, vip, , 990.00",
        "1250, 1, regular, SAVE20PERCENT, 1000.00",
        "200, 11, vip, SAVE10, 1833.58",
        "200, 11, wholesale, SAVE20PERCENT, 1392.78",
        "999999999999999999.99, 1, regular, , 979999999999999999.99",
        "0.004, 1, regular, , 0.00",
        "0.005, 1, regular, , 0.01",
        "0.015, 1, vip, , 0.01",
        "0.005, 11, regular, , 0.05"
    })
    void characterizesSingleLine(String price, int quantity, String customer, String coupon, String expected) {
        BigDecimal total = calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(new BigDecimal(price), quantity)), customer, coupon);

        assertThat(total).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void sumsLinesBeforeRounding() {
        assertThat(calculator.calculateOrderTotal(List.of(
                new PricingCalculator.LineItem(new BigDecimal("0.004"), 1),
                new PricingCalculator.LineItem(new BigDecimal("0.004"), 1)), "regular", null))
                .isEqualTo(new BigDecimal("0.01"));
    }

    @Test
    void appliesQuantityDiscountPerLine() {
        assertThat(calculator.calculateOrderTotal(List.of(
                new PricingCalculator.LineItem(BigDecimal.TEN, 10),
                new PricingCalculator.LineItem(BigDecimal.TEN, 1)), "regular", null))
                .isEqualTo(new BigDecimal("110.00"));
    }

    @Test
    void negativeLinesReducePositiveLinesBeforeClamping() {
        assertThat(calculator.calculateOrderTotal(List.of(
                new PricingCalculator.LineItem(new BigDecimal("-20"), 1),
                new PricingCalculator.LineItem(new BigDecimal("30"), 1)), "regular", null))
                .isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void emptyOrderIsZeroEvenWithCoupon() {
        assertThat(calculator.calculateOrderTotal(List.of(), null, "SAVE10"))
                .isEqualTo(new BigDecimal("0.00"));
    }

    @Test
    void nullInputsThrowInsteadOfBeingIgnored() {
        assertThatThrownBy(() -> calculator.calculateOrderTotal(null, null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calculator.calculateOrderTotal(Arrays.asList((PricingCalculator.LineItem) null), null, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> calculator.calculateOrderTotal(
                List.of(new PricingCalculator.LineItem(null, 0)), null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
