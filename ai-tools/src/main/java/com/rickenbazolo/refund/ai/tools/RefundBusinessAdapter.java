package com.rickenbazolo.refund.ai.tools;

import com.rickenbazolo.refund.ai.tools.RefundToolDtos.DraftView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.EligibilityView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.OrderView;
import com.rickenbazolo.refund.application.EligibilityService;
import com.rickenbazolo.refund.application.OrderService;
import com.rickenbazolo.refund.application.RefundService;
import com.rickenbazolo.refund.domain.AuthenticatedActor;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RefundBusinessAdapter implements RefundBusinessOperations {

  private final OrderService orders;
  private final EligibilityService eligibility;
  private final RefundService refunds;

  public RefundBusinessAdapter(
      OrderService orders, EligibilityService eligibility, RefundService refunds) {
    this.orders = orders;
    this.eligibility = eligibility;
    this.refunds = refunds;
  }

  @Override
  public OrderView getAuthorizedOrder(String userId, String orderId) {
    var order = orders.getAuthorized(orderId, actor(userId));
    return new OrderView(
        order.orderId(),
        order.status().name(),
        order.productType(),
        order.purchaseDate().toString());
  }

  @Override
  public EligibilityView checkEligibility(String userId, String orderId) {
    var order = orders.getAuthorized(orderId, actor(userId));
    var decision = eligibility.evaluate(order);
    String reason =
        decision.reasons().isEmpty()
            ? "Éligible selon la politique " + decision.policyVersion()
            : String.join(", ", decision.reasons());
    if (decision.requiresVerification()) {
      reason += "; vérification requise";
    }
    return new EligibilityView(orderId, decision.eligible(), reason);
  }

  @Override
  public DraftView createDraft(String userId, String orderId) {
    var draft = refunds.prepareDraft(orderId, actor(userId));
    return new DraftView(draft.draftId(), draft.orderId(), draft.status());
  }

  private AuthenticatedActor actor(String userId) {
    if (userId == null || userId.isBlank()) {
      throw new SecurityException("Authenticated user required");
    }
    return new AuthenticatedActor(userId, Set.of(userId));
  }
}
