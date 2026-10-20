package com.rickenbazolo.refund.application;

import com.rickenbazolo.refund.domain.*;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;

public final class EligibilityService {
  private final PolicyRepository policies;
  private final Clock clock;

  public EligibilityService(PolicyRepository policies, Clock clock) {
    this.policies = policies;
    this.clock = clock;
  }

  public EligibilityDecision evaluate(Order order) {
    var policy =
        policies.findApplicable(order.productType(), java.time.LocalDate.now(clock)).orElse(null);
    if (policy == null)
      return new EligibilityDecision(
          false, true, java.util.List.of("NO_APPLICABLE_POLICY"), "NONE");
    var reasons = new ArrayList<String>();
    boolean verification = false;
    if (order.status() != OrderStatus.DELIVERED) reasons.add("ORDER_NOT_DELIVERED");
    long days = ChronoUnit.DAYS.between(order.purchaseDate(), java.time.LocalDate.now(clock));
    if (days < 0 || days > policy.standardWindowDays()) reasons.add("OUTSIDE_STANDARD_WINDOW");
    if (policy.proofRequired() && !order.proofAvailable()) {
      reasons.add("PROOF_REQUIRED");
      verification = true;
    }
    return new EligibilityDecision(reasons.isEmpty(), verification, reasons, policy.version());
  }
}
