package com.rickenbazolo.refund.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.rickenbazolo.refund.domain.RefundIntent;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.messages.UserMessage;

class RefundConversationServiceTest {

  private final ChatMemory memory = MessageWindowChatMemory.builder().maxMessages(20).build();
  private final FakeGateway gateway = new FakeGateway();
  private final RefundConversationService service = new RefundConversationService(gateway, memory);

  @Test
  void shouldKeepConversationsSeparate() {
    memory.add(ConversationScope.key("alice", "conv-1"), new UserMessage("Order CMD-1042"));

    assertEquals(1, service.inspect("alice", "conv-1").storedMessages());
    assertEquals(0, service.inspect("bob", "conv-1").storedMessages());
  }

  @Test
  void shouldClearOnlySelectedConversation() {
    memory.add(ConversationScope.key("alice", "conv-1"), new UserMessage("First conversation"));
    memory.add(ConversationScope.key("alice", "conv-2"), new UserMessage("Second conversation"));

    service.clear("alice", "conv-1");

    assertEquals(0, service.inspect("alice", "conv-1").storedMessages());
    assertEquals(1, service.inspect("alice", "conv-2").storedMessages());
  }

  @Test
  void shouldUseScopedConversationKeyForReply() {
    String reply = service.reply("alice", "conv-1", "What about the invoice?");

    assertEquals("Simulated response", reply);
    assertEquals(ConversationScope.key("alice", "conv-1"), gateway.conversationKey);
    assertEquals("What about the invoice?", gateway.message);
  }

  @Test
  void shouldRejectBlankMessage() {
    assertThrows(IllegalArgumentException.class, () -> service.reply("alice", "conv-1", " "));
  }

  static class FakeGateway implements RefundChatGateway {

    private String conversationKey;
    private String message;

    @Override
    public String chat(String message) {
      return "Test response";
    }

    @Override
    public RefundIntent extractIntent(String message) {
      throw new UnsupportedOperationException();
    }

    @Override
    public String chatWithMemory(String conversationKey, String message) {
      this.conversationKey = conversationKey;
      this.message = message;
      return "Simulated response";
    }
  }
}
