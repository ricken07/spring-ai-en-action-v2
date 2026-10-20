package com.rickenbazolo.refund.application;

import com.rickenbazolo.refund.domain.*;
import java.time.*;
import java.util.Set;

public class ApplicationSmokeTest {
  public static void main(String[] args) {
    var orders = new OrderService(DemoFixtures.orders());
    var policy =
        new EligibilityService(
            DemoFixtures.policies(),
            Clock.fixed(Instant.parse("2026-10-20T12:00:00Z"), ZoneOffset.UTC));
    var alice = new AuthenticatedActor("alice", Set.of("alice"));
    var bob = new AuthenticatedActor("bob", Set.of("bob"));
    check(orders.getAuthorized("CMD-1042", alice).customerId().equals("alice"), "Alice own order");
    try {
      orders.getAuthorized("CMD-2099", alice);
      throw new AssertionError("ACL BYPASS");
    } catch (ForbiddenOrderAccessException expected) {
    }
    check(orders.getAuthorized("CMD-2099", bob).customerId().equals("bob"), "Bob own order");
    check(
        policy.evaluate(orders.getAuthorized("CMD-1042", alice)).eligible(),
        "Recent laptop eligible");
    check(
        !policy.evaluate(orders.getAuthorized("CMD-1043", alice)).eligible(),
        "Accessory outside window");
    var missing = policy.evaluate(orders.getAuthorized("CMD-1050", alice));
    check(!missing.eligible() && missing.requiresVerification(), "Proof required");
    var drafts = new RefundService(orders, policy);
    check(drafts.prepareDraft("CMD-1042", alice).status().equals("DRAFT"), "Draft not paid");
    try {
      drafts.prepareDraft("CMD-2099", alice);
      throw new AssertionError("Draft ACL BYPASS");
    } catch (ForbiddenOrderAccessException expected) {
    }
    try {
      drafts.prepareDraft("CMD-1050", alice);
      throw new AssertionError("Blocked draft created");
    } catch (IllegalStateException expected) {
    }
    System.out.println("PASS 8 business/ACL checks");
  }

  static void check(boolean b, String name) {
    if (!b) throw new AssertionError(name);
  }
}
