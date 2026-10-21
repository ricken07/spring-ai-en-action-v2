package com.rickenbazolo.refund.ai.tools;

import com.rickenbazolo.refund.ai.tools.RefundToolDtos.EligibilityView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.OrderView;
import java.util.Map;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class RefundReadTools {

  static final String AUTHENTICATED_USER_KEY = "authenticatedUserId";
  static final String REQUEST_ID_KEY = "requestId";

  private final RefundBusinessOperations business;
  private final ToolInvocationTracker tracker;

  public RefundReadTools(RefundBusinessOperations business, ToolInvocationTracker tracker) {
    this.business = business;
    this.tracker = tracker;
  }

  @Tool(
      name = "getOrder",
      description =
          "Get the authenticated customer's order summary using its order identifier. "
              + "Never use this tool to access another customer's orders.")
  public OrderView getOrder(
      @ToolParam(description = "Customer order identifier") String orderId, ToolContext context) {
    recordInvocation(context, "getOrder");
    return business.getAuthorizedOrder(authenticatedUser(context), orderId);
  }

  @Tool(
      name = "checkRefundEligibility",
      description =
          "Calculate current refund eligibility for the authenticated customer's order. "
              + "This tool does not create a refund.")
  public EligibilityView checkRefundEligibility(
      @ToolParam(description = "Customer order identifier") String orderId, ToolContext context) {
    recordInvocation(context, "checkRefundEligibility");
    return business.checkEligibility(authenticatedUser(context), orderId);
  }

  private String authenticatedUser(ToolContext context) {
    Object value = contextValue(context, AUTHENTICATED_USER_KEY);
    if (!(value instanceof String userId) || userId.isBlank()) {
      throw new SecurityException("Missing authenticated user");
    }
    return userId;
  }

  private void recordInvocation(ToolContext context, String toolName) {
    Object requestId = contextValue(context, REQUEST_ID_KEY);
    if (requestId instanceof String id && !id.isBlank()) {
      tracker.record(id, toolName);
    }
  }

  private Object contextValue(ToolContext context, String key) {
    if (context == null || context.getContext() == null) {
      return null;
    }
    Map<String, Object> values = context.getContext();
    return values.get(key);
  }
}
