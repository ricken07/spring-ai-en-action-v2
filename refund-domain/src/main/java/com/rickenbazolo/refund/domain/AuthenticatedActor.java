package com.rickenbazolo.refund.domain;

import java.util.Set;

public record AuthenticatedActor(String subjectId, Set<String> allowedCustomerIds) {
  public AuthenticatedActor {
    allowedCustomerIds = Set.copyOf(allowedCustomerIds);
  }

  public boolean canAccess(String customerId) {
    return allowedCustomerIds.contains(customerId);
  }
}
