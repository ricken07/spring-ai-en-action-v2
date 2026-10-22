package com.rickenbazolo.refund.domain;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class OrderInvariantTest {

  @Test
  void shouldRejectNegativeAmount() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            new Order(
                "id",
                "alice",
                "LAPTOP",
                LocalDate.now(),
                OrderStatus.DELIVERED,
                new BigDecimal("-1"),
                true));
  }
}
