package com.rickenbazolo.refund.ai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ChatClientConfig {

  private static final String SYSTEM_PROMPT =
      """
      You are an assistant for refund requests.

      Clarify the customer's request when necessary.
      Never invent order details.
      Never claim a refund has been approved.
      Never claim that a payment has been executed.
      Respond in the user's language.
      """;

  @Bean
  ChatClient refundChatClient(ChatClient.Builder builder) {
    return builder.defaultSystem(SYSTEM_PROMPT).build();
  }

  @Bean
  ChatClient refundConversationClient(
      ChatClient.Builder builder, @Qualifier("refundChatMemory") ChatMemory memory) {
    return builder
        .defaultSystem(SYSTEM_PROMPT)
        .defaultAdvisors(MessageChatMemoryAdvisor.builder(memory).build())
        .build();
  }
}
