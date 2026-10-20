package com.rickenbazolo.refund.ai.rag;

import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RefundRagService {

  private final ChatClient chatClient;
  private final RefundPolicySearchService searchService;

  public RefundRagService(
      @Qualifier("refundRagChatClient") ChatClient chatClient,
      RefundPolicySearchService searchService) {
    this.chatClient = chatClient;
    this.searchService = searchService;
  }

  public RagAnswer ask(String question) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("Question must not be blank");
    }

    var documents = searchService.search(question);
    if (documents.isEmpty()) {
      return new RagAnswer(question, "Aucune politique pertinente n'a été retrouvée.", List.of());
    }

    String answer = chatClient.prompt().user(question).call().content();
    var references =
        documents.stream()
            .map(
                document ->
                    new RagAnswer.RetrievedPolicy(
                        String.valueOf(document.getMetadata().getOrDefault("policyId", "")),
                        String.valueOf(document.getMetadata().getOrDefault("source", "")),
                        document.getText(),
                        document.getScore()))
            .toList();

    return new RagAnswer(question, answer, references);
  }
}
