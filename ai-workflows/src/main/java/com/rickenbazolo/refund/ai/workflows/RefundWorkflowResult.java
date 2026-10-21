package com.rickenbazolo.refund.ai.workflows;

import com.rickenbazolo.refund.ai.tools.RefundToolDtos.DraftView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.EligibilityView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.OrderView;
import com.rickenbazolo.refund.domain.RefundIntent;
import java.util.List;

public record RefundWorkflowResult(
    String status,
    RefundIntent intent,
    OrderView order,
    EligibilityView eligibility,
    DraftView draft,
    List<String> executedSteps) {

  public RefundWorkflowResult {
    executedSteps = List.copyOf(executedSteps);
  }
}
