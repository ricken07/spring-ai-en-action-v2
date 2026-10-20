package com.rickenbazolo.refund.ai.rag.advanced;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class ModularRagService {

  private final ChatClient chatClient;

  public ModularRagService(@Qualifier("modularRagChatClient") ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  public String ask(String question) {
    if (question == null || question.isBlank()) {
      throw new IllegalArgumentException("Question must not be blank");
    }
    return chatClient.prompt().user(question).call().content();
  }
}
