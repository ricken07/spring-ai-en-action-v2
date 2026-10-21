package com.rickenbazolo.refund.ai.tools;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RefundToolChatService {

  private final ChatClient client;
  private final RefundReadTools tools;
  private final ToolInvocationTracker tracker;

  public RefundToolChatService(
      @Qualifier("refundToolChatClient") ChatClient client,
      RefundReadTools tools,
      ToolInvocationTracker tracker) {
    this.client = client;
    this.tools = tools;
    this.tracker = tracker;
  }

  public ToolChatResult ask(String authenticatedUserId, String question) {
    if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
      throw new SecurityException("Authenticated user required");
    }
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("Question must not be blank");
    }

    String requestId = UUID.randomUUID().toString();
    try {
      String answer =
          client
              .prompt()
              .user(question)
              .tools(tools)
              .toolContext(
                  Map.of(
                      RefundReadTools.AUTHENTICATED_USER_KEY, authenticatedUserId,
                      RefundReadTools.REQUEST_ID_KEY, requestId))
              .call()
              .content();
      return new ToolChatResult(requestId, answer, tracker.drain(requestId));
    } catch (RuntimeException exception) {
      tracker.drain(requestId);
      throw exception;
    }
  }

  public record ToolChatResult(String requestId, String answer, List<String> invokedTools) {
    public ToolChatResult {
      invokedTools = List.copyOf(invokedTools);
    }
  }
}
