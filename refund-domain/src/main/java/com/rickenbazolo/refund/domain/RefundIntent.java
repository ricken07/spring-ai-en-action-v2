package com.rickenbazolo.refund.domain;

import java.util.List;

public record RefundIntent(
    String orderId, String reason, String requestedAction, List<String> missingFields) {
  public RefundIntent {
    missingFields = List.copyOf(missingFields);
  }
}
