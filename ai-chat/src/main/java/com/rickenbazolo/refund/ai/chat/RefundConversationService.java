package com.rickenbazolo.refund.ai.chat;

import java.util.List;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
public class RefundConversationService {

  private final RefundChatGateway gateway;
  private final ChatMemory memory;

  public RefundConversationService(
      RefundChatGateway gateway, @Qualifier("refundChatMemory") ChatMemory memory) {
    this.gateway = gateway;
    this.memory = memory;
  }

  public String reply(String userId, String conversationId, String message) {
    requireMessage(message);
    String key = ConversationScope.key(userId, conversationId);
    return gateway.chatWithMemory(key, message);
  }

  public ConversationSnapshot inspect(String userId, String conversationId) {
    String key = ConversationScope.key(userId, conversationId);
    var messages = memory.get(key);
    List<String> messageTypes =
        messages.stream().map(message -> message.getMessageType().name()).toList();

    return new ConversationSnapshot(conversationId, messages.size(), messageTypes);
  }

  public void clear(String userId, String conversationId) {
    memory.clear(ConversationScope.key(userId, conversationId));
  }

  private void requireMessage(String message) {
    if (message == null || message.isBlank()) {
      throw new IllegalArgumentException("Message must not be blank");
    }
  }
}
