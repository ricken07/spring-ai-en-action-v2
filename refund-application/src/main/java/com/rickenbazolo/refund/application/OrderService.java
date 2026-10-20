package com.rickenbazolo.refund.application;

import com.rickenbazolo.refund.domain.*;
import java.util.NoSuchElementException;
import java.util.Objects;

public final class OrderService {
  private final OrderRepository repository;

  public OrderService(OrderRepository repository) {
    this.repository = Objects.requireNonNull(repository);
  }

  public Order getAuthorized(String orderId, AuthenticatedActor actor) {
    Objects.requireNonNull(actor, "Authenticated actor required");
    var order =
        repository
            .findById(orderId)
            .orElseThrow(() -> new NoSuchElementException("Order not found"));
    if (!actor.canAccess(order.customerId())) throw new ForbiddenOrderAccessException();
    return order;
  }
}
