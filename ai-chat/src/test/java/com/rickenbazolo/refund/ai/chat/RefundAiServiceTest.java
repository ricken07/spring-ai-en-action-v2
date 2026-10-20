package com.rickenbazolo.refund.ai.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rickenbazolo.refund.domain.RefundIntent;
import java.util.List;
import org.junit.jupiter.api.Test;

class RefundAiServiceTest {

  private final FakeGateway gateway = new FakeGateway();
  private final RefundAiService service = new RefundAiService(gateway, new RefundIntentValidator());

  @Test
  void shouldExtractValidRefundIntent() {
    gateway.intent = new RefundIntent("CMD-1042", "Computer is defective", "REFUND", List.of());

    var result = service.analyze("Je veux rembourser CMD-1042");

    assertTrue(result.valid());
    assertEquals("CMD-1042", result.intent().orderId());
    assertTrue(result.errors().isEmpty());
  }

  @Test
  void shouldRejectMissingOrderIdentifier() {
    gateway.intent = new RefundIntent("", "Computer is defective", "REFUND", List.of("orderId"));

    var result = service.analyze("Mon ordinateur ne marche plus");

    assertFalse(result.valid());
    assertTrue(result.errors().contains("Missing order identifier"));
  }

  @Test
  void shouldRejectEmptyMessage() {
    assertThrows(IllegalArgumentException.class, () -> service.analyze(" "));
  }

  static class FakeGateway implements RefundChatGateway {

    RefundIntent intent;

    @Override
    public String chat(String message) {
      return "Test response";
    }

    @Override
    public RefundIntent extractIntent(String message) {
      return intent;
    }
  }
}
