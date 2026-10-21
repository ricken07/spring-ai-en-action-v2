package com.rickenbazolo.refund.ai.tools;

import static org.junit.jupiter.api.Assertions.*;

import com.rickenbazolo.refund.application.DemoFixtures;
import com.rickenbazolo.refund.application.EligibilityService;
import com.rickenbazolo.refund.application.ForbiddenOrderAccessException;
import com.rickenbazolo.refund.application.OrderService;
import com.rickenbazolo.refund.application.RefundService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class RefundBusinessAdapterTest {

  private final OrderService orders = new OrderService(DemoFixtures.orders());
  private final EligibilityService eligibility =
      new EligibilityService(
          DemoFixtures.policies(),
          Clock.fixed(Instant.parse("2026-10-21T00:00:00Z"), ZoneOffset.UTC));
  private final RefundBusinessAdapter adapter =
      new RefundBusinessAdapter(orders, eligibility, new RefundService(orders, eligibility));

  @Test
  void shouldReadAndEvaluateAuthorizedOrderUsingDomainServices() {
    var order = adapter.getAuthorizedOrder("alice", "CMD-1042");
    var decision = adapter.checkEligibility("alice", "CMD-1042");

    assertEquals("DELIVERED", order.status());
    assertEquals("LAPTOP", order.product());
    assertTrue(decision.eligible());
  }

  @Test
  void shouldRejectAnotherCustomersOrder() {
    assertThrows(
        ForbiddenOrderAccessException.class, () -> adapter.getAuthorizedOrder("bob", "CMD-1042"));
    assertThrows(
        ForbiddenOrderAccessException.class, () -> adapter.checkEligibility("bob", "CMD-1042"));
    assertThrows(ForbiddenOrderAccessException.class, () -> adapter.createDraft("bob", "CMD-1042"));
  }

  @Test
  void shouldRecheckEligibilityBeforeDraftCreation() {
    assertThrows(IllegalStateException.class, () -> adapter.createDraft("alice", "CMD-1050"));
  }
}
