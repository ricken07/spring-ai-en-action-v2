package com.rickenbazolo.refund.ai.chat;

import com.rickenbazolo.refund.domain.RefundIntent;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class SpringAiRefundChatGateway implements RefundChatGateway {

  private final ChatClient chatClient;
  private final ChatClient conversationClient;

  public SpringAiRefundChatGateway(
      @Qualifier("refundChatClient") ChatClient chatClient,
      @Qualifier("refundConversationClient") ChatClient conversationClient) {
    this.chatClient = chatClient;
    this.conversationClient = conversationClient;
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

  @Override
  public String chatWithMemory(String conversationKey, String message) {
    return conversationClient
        .prompt()
        .user(message)
        .advisors(advisor -> advisor.param(ChatMemory.CONVERSATION_ID, conversationKey))
        .call()
        .content();
  }
}
