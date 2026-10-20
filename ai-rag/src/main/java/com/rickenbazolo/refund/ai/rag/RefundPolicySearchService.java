package com.rickenbazolo.refund.ai.rag;

import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RefundPolicySearchService {

  private final VectorStore vectorStore;

  public RefundPolicySearchService(VectorStore vectorStore) {
    this.vectorStore = vectorStore;
  }

  public List<Document> search(String question) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("Question must not be blank");
    }

    var request =
        SearchRequest.builder()
            .query(question)
            .topK(4)
            .similarityThreshold(0.0)
            .filterExpression("category == 'refund-policy'")
            .build();

    return vectorStore.similaritySearch(request);
  }
}
