package com.rickenbazolo.refund.mcpserver;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class RefundPolicyToolsTest {

  private final RefundPolicyTools tools = new RefundPolicyTools(new RefundPolicyService());

  @Test
  void shouldRetrieveKnownPolicy() {
    var policy = tools.getRefundPolicy("REF-PROOF-001");

    assertEquals("REF-PROOF-001", policy.policyId());
    assertEquals("1.0", policy.version());
    assertTrue(policy.content().contains("confirmation de commande"));
  }

  @Test
  void shouldRejectUnknownPolicy() {
    assertThrows(IllegalArgumentException.class, () -> tools.getRefundPolicy("UNKNOWN-001"));
  }

  @Test
  void shouldNotExposeCustomerOrderData() {
    var policy = tools.getRefundPolicy("REF-DEFECT-001");

    assertNotNull(policy.content());
    assertFalse(policy.content().contains("CMD-1042"));
  }
}
