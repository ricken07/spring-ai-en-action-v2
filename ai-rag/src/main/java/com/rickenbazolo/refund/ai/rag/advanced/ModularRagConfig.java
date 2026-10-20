package com.rickenbazolo.refund.ai.rag.advanced;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionTextParser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Spring AI's composable RAG API, kept separate from the instrumented pipeline. */
@Configuration(proxyBeanMethods = false)
public class ModularRagConfig {

  @Bean
  RetrievalAugmentationAdvisor refundModularRagAdvisor(VectorStore vectorStore) {
    return RetrievalAugmentationAdvisor.builder()
        .documentRetriever(
            VectorStoreDocumentRetriever.builder()
                .vectorStore(vectorStore)
                .topK(10)
                .similarityThreshold(0.0)
                .filterExpression(
                    new FilterExpressionTextParser().parse("category == 'refund-policy'"))
                .build())
        .build();
  }

  @Bean("modularRagChatClient")
  ChatClient modularRagChatClient(
      ChatClient.Builder builder, RetrievalAugmentationAdvisor refundModularRagAdvisor) {
    return builder
        .defaultSystem(
            """
            Answer refund policy questions using retrieved policy context.
            Do not invent policy exceptions or decide refund eligibility.
            Respond in the same language as the user's question.
            """)
        .defaultAdvisors(refundModularRagAdvisor)
        .build();
  }
}
