package com.rickenbazolo.refund.mcpserver;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class RefundPolicyTools {

  private final RefundPolicyService service;

  public RefundPolicyTools(RefundPolicyService service) {
    this.service = service;
  }

  @Tool(
      name = "getRefundPolicy",
      description =
          "Retrieve a fictional refund policy by its exact identifier. Read-only; this tool does"
              + " not determine customer eligibility.")
  public RefundPolicy getRefundPolicy(
      @ToolParam(description = "Exact refund policy identifier") String policyId) {
    return service
        .findById(policyId)
        .orElseThrow(() -> new IllegalArgumentException("Unknown refund policy"));
  }
}
