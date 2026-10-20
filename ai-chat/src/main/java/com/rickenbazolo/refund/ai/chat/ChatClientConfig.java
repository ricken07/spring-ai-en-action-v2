package com.rickenbazolo.refund.ai.chat;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ChatClientConfig {

  @Bean
  ChatClient refundChatClient(ChatClient.Builder builder) {
    return builder
        .defaultSystem(
            """
            You are an assistant for refund requests.

            Your role is to understand the customer's request and clarify missing information.

            Never claim a refund is approved.
            Never claim a payment was executed.
            Do not invent order details.
            Respond in the user's language.
            """)
        .build();
  }
}
