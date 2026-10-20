package com.rickenbazolo.refund.ai.rag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

class RefundPolicySearchServiceTest {

  private final VectorStore vectorStore = mock(VectorStore.class);
  private final RefundPolicySearchService service = new RefundPolicySearchService(vectorStore);

  @Test
  void shouldRetrieveWithExpectedQuery() {
    when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

    service.search("Facture perdue");

    var captor = ArgumentCaptor.forClass(SearchRequest.class);
    verify(vectorStore).similaritySearch(captor.capture());

    var request = captor.getValue();
    assertEquals("Facture perdue", request.getQuery());
    assertEquals(4, request.getTopK());
    assertNotNull(request.getFilterExpression());
  }

  @Test
  void shouldRejectBlankQuestion() {
    assertThrows(IllegalArgumentException.class, () -> service.search(" "));
    verifyNoInteractions(vectorStore);
  }
}
