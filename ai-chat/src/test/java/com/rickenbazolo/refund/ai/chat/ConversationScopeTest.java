package com.rickenbazolo.refund.ai.chat;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ConversationScopeTest {

  @Test
  void shouldIsolateUsers() {
    String alice = ConversationScope.key("alice", "conv-001");
    String bob = ConversationScope.key("bob", "conv-001");

    assertNotEquals(alice, bob);
  }

  @Test
  void shouldIsolateConversations() {
    String first = ConversationScope.key("alice", "conv-001");
    String second = ConversationScope.key("alice", "conv-002");

    assertNotEquals(first, second);
  }

  @Test
  void shouldRejectInvalidIdentifier() {
    assertThrows(IllegalArgumentException.class, () -> ConversationScope.key("alice", "../other"));
  }
}
