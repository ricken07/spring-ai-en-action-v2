package com.rickenbazolo.refund.ai.workflows;

import com.rickenbazolo.refund.ai.chat.RefundChatGateway;
import com.rickenbazolo.refund.ai.chat.RefundIntentValidator;
import com.rickenbazolo.refund.ai.tools.RefundBusinessOperations;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.DraftView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.EligibilityView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.OrderView;
import com.rickenbazolo.refund.domain.RefundIntent;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RefundWorkflowService {

  private final RefundChatGateway chatGateway;
  private final RefundIntentValidator validator;
  private final RefundBusinessOperations business;

  public RefundWorkflowService(
      RefundChatGateway chatGateway,
      RefundIntentValidator validator,
      RefundBusinessOperations business) {
    this.chatGateway = chatGateway;
    this.validator = validator;
    this.business = business;
  }

  public RefundWorkflowResult execute(String userId, RefundWorkflowRequest request) {
    if (userId == null || userId.isBlank()) {
      throw new SecurityException("Authenticated user required");
    }
    if (request == null || request.message() == null || request.message().isBlank()) {
      throw new IllegalArgumentException("Message must not be blank");
    }

    List<String> steps = new ArrayList<>();
    RefundIntent intent = chatGateway.extractIntent(request.message());
    steps.add("EXTRACT_INTENT");

    List<String> errors = validator.validate(intent);
    if (!errors.isEmpty()) {
      steps.add("VALIDATION_FAILED");
      return result("NEEDS_CLARIFICATION", intent, null, null, null, steps);
    }
    if (!"REFUND".equals(intent.requestedAction())) {
      steps.add("NON_REFUND_INTENT");
      return result("UNSUPPORTED_INTENT", intent, null, null, null, steps);
    }
    steps.add("VALIDATE_INTENT");

    OrderView order = business.getAuthorizedOrder(userId, intent.orderId());
    steps.add("GET_AUTHORIZED_ORDER");

    EligibilityView eligibility = business.checkEligibility(userId, intent.orderId());
    steps.add("CHECK_ELIGIBILITY");
    if (!eligibility.eligible()) {
      return result("NOT_ELIGIBLE", intent, order, eligibility, null, steps);
    }

    if (!request.confirmed()) {
      steps.add("AWAIT_CONFIRMATION");
      return result("CONFIRMATION_REQUIRED", intent, order, eligibility, null, steps);
    }

    // The business operation must recheck authorization and eligibility at write time.
    DraftView draft = business.createDraft(userId, intent.orderId());
    steps.add("CREATE_DRAFT");
    return result("DRAFT_CREATED", intent, order, eligibility, draft, steps);
  }

  private RefundWorkflowResult result(
      String status,
      RefundIntent intent,
      OrderView order,
      EligibilityView eligibility,
      DraftView draft,
      List<String> steps) {
    return new RefundWorkflowResult(status, intent, order, eligibility, draft, steps);
  }
}
