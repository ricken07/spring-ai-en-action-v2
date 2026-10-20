package com.rickenbazolo.refund.ai.chat;

import java.util.List;

public record ConversationSnapshot(
    String conversationId, int storedMessages, List<String> messageTypes) {

  public ConversationSnapshot {
    messageTypes = List.copyOf(messageTypes);
  }
}
