package com.rickenbazolo.refund.application;

import com.rickenbazolo.refund.domain.*;
import java.util.UUID;

public final class RefundService {
  private final OrderService orders;
  private final EligibilityService eligibility;

  public RefundService(OrderService orders, EligibilityService eligibility) {
    this.orders = orders;
    this.eligibility = eligibility;
  }

  public RefundDraft prepareDraft(String orderId, AuthenticatedActor actor) {
    var order = orders.getAuthorized(orderId, actor);
    var decision = eligibility.evaluate(order);
    if (!decision.eligible())
      throw new IllegalStateException("Refund draft blocked: " + decision.reasons());
    return new RefundDraft(UUID.randomUUID().toString(), orderId, "DRAFT", decision);
  }
}
