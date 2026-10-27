package com.rbdip.bookstore.order;

import java.math.BigDecimal;

class OrderEmailSender {

    void sendConfirmation(String customerName, Long orderId, BigDecimal total) {
        System.out.printf(
                "[email] Dear %s, your order #%d for %s has been placed.%n", customerName, orderId, total);
    }
}
