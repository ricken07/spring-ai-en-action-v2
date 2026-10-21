package com.rickenbazolo.refund.ai.tools;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RefundToolConfig {

  @Bean
  RefundReadTools refundReadTools(
      RefundBusinessOperations business, ToolInvocationTracker tracker) {
    return new RefundReadTools(business, tracker);
  }

  @Bean
  ChatClient refundToolChatClient(ChatClient.Builder builder) {
    return builder
        .defaultSystem(
            """
            You are a refund management assistant.
            Use the available tools to consult actual order data and calculate eligibility.
            Never invent order status or eligibility.
            Never claim a refund was executed or a draft was created.
            Respond in the user's language.
            """)
        .build();
  }
}
