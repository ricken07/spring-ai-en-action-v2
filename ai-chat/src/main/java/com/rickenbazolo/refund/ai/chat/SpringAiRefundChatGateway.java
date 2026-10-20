package com.rickenbazolo.refund.ai.chat;

import com.rickenbazolo.refund.domain.RefundIntent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SpringAiRefundChatGateway implements RefundChatGateway {

  private final ChatClient chatClient;

  public SpringAiRefundChatGateway(@Qualifier("refundChatClient") ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public String chat(String message) {
    return chatClient.prompt().user(message).call().content();
  }

  @Override
  public RefundIntent extractIntent(String message) {
    return chatClient
        .prompt()
        .system(
            """
            Extract the customer's refund intent.

            Return the following information:
            - orderId: order identifier, or empty string
            - reason: reason for the refund
            - requestedAction: REFUND, INFORMATION or OTHER
            - missingFields: missing information as a list

            Do not invent missing information.
            Do not decide refund eligibility.
            """)
        .user(message)
        .call()
        .entity(RefundIntent.class);
  }
}
