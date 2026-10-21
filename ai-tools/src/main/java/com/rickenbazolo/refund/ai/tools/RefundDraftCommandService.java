package com.rickenbazolo.refund.ai.tools;

import com.rickenbazolo.refund.ai.tools.RefundToolDtos.DraftView;
import org.springframework.stereotype.Service;

@Service
public class RefundDraftCommandService {

  private final RefundBusinessOperations business;

  public RefundDraftCommandService(RefundBusinessOperations business) {
    this.business = business;
  }

  public DraftView prepareDraft(String authenticatedUserId, String orderId, boolean confirmed) {
    if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
      throw new SecurityException("Authenticated user required");
    }
    if (orderId == null || orderId.isBlank()) {
      throw new IllegalArgumentException("Order identifier must not be blank");
    }
    if (!confirmed) {
      throw new IllegalStateException("Explicit confirmation is required");
    }
    return business.createDraft(authenticatedUserId, orderId);
  }
}
