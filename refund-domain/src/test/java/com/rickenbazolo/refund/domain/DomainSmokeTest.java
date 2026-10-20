package com.rickenbazolo.refund.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

public class DomainSmokeTest {
  public static void main(String[] args) {
    try {
      new Order(
          "id",
          "alice",
          "LAPTOP",
          LocalDate.now(),
          OrderStatus.DELIVERED,
          new BigDecimal("-1"),
          true);
      throw new AssertionError("Negative amount allowed");
    } catch (IllegalArgumentException expected) {
    }
    System.out.println("PASS domain invariants");
  }
}
