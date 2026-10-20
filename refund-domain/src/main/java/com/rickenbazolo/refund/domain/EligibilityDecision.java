package com.rickenbazolo.refund.domain;

import java.util.List;

public record EligibilityDecision(
    boolean eligible, boolean requiresVerification, List<String> reasons, String policyVersion) {
  public EligibilityDecision {
    reasons = List.copyOf(reasons);
  }
}
