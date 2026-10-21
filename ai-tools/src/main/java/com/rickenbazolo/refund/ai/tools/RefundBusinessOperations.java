package com.rickenbazolo.refund.ai.tools;

import com.rickenbazolo.refund.ai.tools.RefundToolDtos.DraftView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.EligibilityView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.OrderView;

public interface RefundBusinessOperations {

  OrderView getAuthorizedOrder(String userId, String orderId);

  EligibilityView checkEligibility(String userId, String orderId);

  DraftView createDraft(String userId, String orderId);
}
