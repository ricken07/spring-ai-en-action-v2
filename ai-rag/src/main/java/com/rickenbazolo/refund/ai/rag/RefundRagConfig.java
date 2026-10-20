package com.rickenbazolo.refund.ai.rag;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RefundRagConfig {

  @Bean
  ChatClient refundRagChatClient(ChatClient.Builder builder, VectorStore vectorStore) {
    var search =
        SearchRequest.builder()
            .topK(4)
            .similarityThreshold(0.0)
            .filterExpression("category == 'refund-policy'")
            .build();

    var advisor = QuestionAnswerAdvisor.builder(vectorStore).searchRequest(search).build();

    return builder
        .defaultSystem(
            """
            You answer questions about refund policies.

            Use the retrieved policy context.
            If the information is insufficient, clearly state what is unknown.

            Do not invent policy exceptions.
            Do not decide refund eligibility.
            Never claim a refund was executed.

            Respond in the same language as the user's question.
            If the question is in French, answer entirely in French.
            """)
        .defaultAdvisors(advisor)
        .build();
  }
}
