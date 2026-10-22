package com.rickenbazolo.refund.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rickenbazolo.refund.domain.AuthenticatedActor;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RefundApplicationTest {

  private final OrderService orders = new OrderService(DemoFixtures.orders());
  private final EligibilityService eligibility =
      new EligibilityService(
          DemoFixtures.policies(),
          Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC));
  private final AuthenticatedActor alice = new AuthenticatedActor("alice", Set.of("alice"));
  private final AuthenticatedActor bob = new AuthenticatedActor("bob", Set.of("bob"));
  private RefundService refunds;

  @BeforeEach
  void setUp() {
    refunds = new RefundService(orders, eligibility);
  }

  @Test
  void shouldAllowOwnerAndRejectOtherCustomer() {
    assertEquals("alice", orders.getAuthorized("CMD-1042", alice).customerId());
    assertThrows(
        ForbiddenOrderAccessException.class, () -> orders.getAuthorized("CMD-2099", alice));
    assertEquals("bob", orders.getAuthorized("CMD-2099", bob).customerId());
  }

  @Test
  void shouldApplyEligibilityRulesDeterministically() {
    assertTrue(eligibility.evaluate(orders.getAuthorized("CMD-1042", alice)).eligible());
    assertFalse(eligibility.evaluate(orders.getAuthorized("CMD-1043", alice)).eligible());
    var missingProof = eligibility.evaluate(orders.getAuthorized("CMD-1050", alice));
    assertFalse(missingProof.eligible());
    assertTrue(missingProof.requiresVerification());
  }

  @Test
  void shouldCreateDraftWithoutExecutingPayment() {
    var draft = refunds.prepareDraft("CMD-1042", alice);
    assertEquals("DRAFT", draft.status());
  }

  @Test
  void shouldRecheckAuthorizationWhenPreparingDraft() {
    assertThrows(
        ForbiddenOrderAccessException.class, () -> refunds.prepareDraft("CMD-2099", alice));
  }

  @Test
  void shouldNotCreateDraftWhenOrderIsNotEligible() {
    assertThrows(IllegalStateException.class, () -> refunds.prepareDraft("CMD-1050", alice));
  }
}
