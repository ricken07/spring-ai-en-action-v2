package com.rickenbazolo.refund.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record Order(
    String orderId,
    String customerId,
    String productType,
    LocalDate purchaseDate,
    OrderStatus status,
    BigDecimal totalAmount,
    boolean proofAvailable) {
  public Order {
    Objects.requireNonNull(orderId);
    Objects.requireNonNull(customerId);
    Objects.requireNonNull(productType);
    Objects.requireNonNull(purchaseDate);
    Objects.requireNonNull(status);
    Objects.requireNonNull(totalAmount);
    if (totalAmount.signum() < 0) throw new IllegalArgumentException("Negative amount");
  }
}
