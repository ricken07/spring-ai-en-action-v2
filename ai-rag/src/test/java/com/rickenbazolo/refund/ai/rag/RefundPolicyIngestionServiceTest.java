package com.rickenbazolo.refund.ai.rag;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;

class RefundPolicyIngestionServiceTest {

  private final VectorStore vectorStore = mock(VectorStore.class);
  private final RefundPolicyIngestionService service =
      new RefundPolicyIngestionService(new RefundPolicyCatalog(), vectorStore);

  @Test
  void shouldPreparePoliciesWithMetadata() {
    var chunks = service.prepareDocuments();

    assertFalse(chunks.isEmpty());
    assertTrue(
        chunks.stream().allMatch(document -> document.getMetadata().containsKey("policyId")));
    assertTrue(
        chunks.stream()
            .allMatch(document -> "refund-policy".equals(document.getMetadata().get("category"))));
    assertTrue(chunks.stream().allMatch(document -> document.getMetadata().containsKey("source")));
  }

  @Test
  void shouldWriteChunksToVectorStore() {
    int count = service.ingest();

    assertTrue(count >= 3);
    verify(vectorStore, times(1)).add(anyList());
  }
}
