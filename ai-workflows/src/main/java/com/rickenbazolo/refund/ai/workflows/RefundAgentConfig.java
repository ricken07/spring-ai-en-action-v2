package com.rickenbazolo.refund.ai.workflows;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RefundAgentConfig {

  @Bean
  ChatClient refundInvestigationClient(ChatClient.Builder builder) {
    return builder
        .defaultSystem(
            """
            You investigate refund requests.

            You may consult authorized order data and ask the application to calculate refund
            eligibility. Use the available read-only tools when factual order data is required.

            Never invent order details or eligibility decisions. Never claim a draft was created.
            Never initiate a refund or payment. Stop when sufficient evidence exists. If a tool
            fails, explain the limitation. Respond in the user's language.
            """)
        .build();
  }
}
