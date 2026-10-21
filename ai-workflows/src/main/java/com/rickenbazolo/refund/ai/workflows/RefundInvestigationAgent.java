package com.rickenbazolo.refund.ai.workflows;

import com.rickenbazolo.refund.ai.tools.RefundReadTools;
import com.rickenbazolo.refund.ai.tools.ToolInvocationTracker;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RefundInvestigationAgent {

  private final ChatClient chatClient;
  private final RefundReadTools readTools;
  private final ToolInvocationTracker tracker;

  public RefundInvestigationAgent(
      @Qualifier("refundInvestigationClient") ChatClient chatClient,
      RefundReadTools readTools,
      ToolInvocationTracker tracker) {
    this.chatClient = chatClient;
    this.readTools = readTools;
    this.tracker = tracker;
  }

  public RefundAgentResult investigate(String authenticatedUserId, String question) {
    if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
      throw new SecurityException("Authenticated user required");
    }
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("Question must not be blank");
    }

    String requestId = UUID.randomUUID().toString();
    try {
      String answer =
          chatClient
              .prompt()
              .user(question)
              .tools(readTools)
              .toolContext(
                  Map.of(
                      "authenticatedUserId", authenticatedUserId,
                      "requestId", requestId))
              .call()
              .content();
      return new RefundAgentResult(answer, "AGENTIC_LOOP", tracker.drain(requestId));
    } catch (RuntimeException exception) {
      tracker.drain(requestId);
      throw exception;
    }
  }
}
