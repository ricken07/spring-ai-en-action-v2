package com.rickenbazolo.refund.application;

import com.rickenbazolo.refund.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class DemoFixtures {
  private DemoFixtures() {}

  public static OrderRepository orders() {
    var rows =
        Map.of(
            "CMD-1042",
                new Order(
                    "CMD-1042",
                    "alice",
                    "LAPTOP",
                    LocalDate.of(2026, 10, 10),
                    OrderStatus.DELIVERED,
                    new BigDecimal("899.00"),
                    true),
            "CMD-1043",
                new Order(
                    "CMD-1043",
                    "alice",
                    "ACCESSORY",
                    LocalDate.of(2026, 8, 1),
                    OrderStatus.DELIVERED,
                    new BigDecimal("39.00"),
                    true),
            "CMD-2099",
                new Order(
                    "CMD-2099",
                    "bob",
                    "LAPTOP",
                    LocalDate.of(2026, 10, 11),
                    OrderStatus.DELIVERED,
                    new BigDecimal("1199.00"),
                    true),
            "CMD-1050",
                new Order(
                    "CMD-1050",
                    "alice",
                    "LAPTOP",
                    LocalDate.of(2026, 10, 12),
                    OrderStatus.DELIVERED,
                    new BigDecimal("749.00"),
                    false));
    return id -> Optional.ofNullable(rows.get(id));
  }

  public static PolicyRepository policies() {
    var rows =
        List.of(
            new RefundPolicy(
                "P-LAPTOP",
                "1.0",
                LocalDate.of(2026, 1, 1),
                "LAPTOP",
                30,
                true,
                "POLICY-LAPTOP-V1.md"),
            new RefundPolicy(
                "P-ACCESSORY",
                "1.0",
                LocalDate.of(2026, 1, 1),
                "ACCESSORY",
                14,
                false,
                "POLICY-ACCESSORY-V1.md"));
    return (type, date) ->
        rows.stream()
            .filter(p -> p.productType().equals(type) && !p.effectiveFrom().isAfter(date))
            .max(Comparator.comparing(RefundPolicy::effectiveFrom));
  }
}
