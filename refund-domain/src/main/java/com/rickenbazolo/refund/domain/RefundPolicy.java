package com.rickenbazolo.refund.domain;

import java.time.LocalDate;
import java.util.Objects;

public record RefundPolicy(
    String policyId,
    String version,
    LocalDate effectiveFrom,
    String productType,
    int standardWindowDays,
    boolean proofRequired,
    String sourceReference) {
  public RefundPolicy {
    Objects.requireNonNull(version);
    Objects.requireNonNull(effectiveFrom);
    if (standardWindowDays < 0) throw new IllegalArgumentException("Negative window");
  }
}
