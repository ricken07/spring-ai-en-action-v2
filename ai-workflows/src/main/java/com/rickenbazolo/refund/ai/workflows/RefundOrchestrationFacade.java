package com.rickenbazolo.refund.ai.workflows;

import org.springframework.stereotype.Service;

@Service
public class RefundOrchestrationFacade {

  private final RefundWorkflowService workflow;
  private final RefundInvestigationAgent agent;

  public RefundOrchestrationFacade(RefundWorkflowService workflow, RefundInvestigationAgent agent) {
    this.workflow = workflow;
    this.agent = agent;
  }

  public Object execute(String userId, String mode, String message, boolean confirmed) {
    if (mode == null) {
      throw new IllegalArgumentException("Unsupported orchestration mode");
    }
    return switch (mode) {
      case "WORKFLOW" -> workflow.execute(userId, new RefundWorkflowRequest(message, confirmed));
      case "AGENT" -> {
        if (confirmed) {
          throw new IllegalArgumentException("Agent mode does not support draft confirmation");
        }
        yield agent.investigate(userId, message);
      }
      default -> throw new IllegalArgumentException("Unsupported orchestration mode");
    };
  }
}
