package com.rickenbazolo.refund.ai.tools;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;

class RefundReadToolsTest {

  private final RefundBusinessOperations business = mock(RefundBusinessOperations.class);
  private final ToolInvocationTracker tracker = new ToolInvocationTracker();
  private final RefundReadTools tools = new RefundReadTools(business, tracker);

  @Test
  void shouldForwardTrustedIdentityAndRecordToolName() {
    var context = new ToolContext(Map.of("authenticatedUserId", "alice", "requestId", "request-1"));
    var expected = new RefundToolDtos.OrderView("CMD-1042", "DELIVERED", "LAPTOP", "2026-10-10");
    when(business.getAuthorizedOrder("alice", "CMD-1042")).thenReturn(expected);

    var result = tools.getOrder("CMD-1042", context);

    assertEquals(expected, result);
    assertEquals(List.of("getOrder"), tracker.drain("request-1"));
    verify(business).getAuthorizedOrder("alice", "CMD-1042");
  }

  @Test
  void shouldRejectMissingIdentityBeforeCallingBusiness() {
    var context = new ToolContext(Map.of("requestId", "request-2"));

    assertThrows(SecurityException.class, () -> tools.getOrder("CMD-1042", context));

    verifyNoInteractions(business);
  }

  @Test
  void shouldPropagateBusinessAuthorizationDenial() {
    var context = new ToolContext(Map.of("authenticatedUserId", "bob"));
    when(business.getAuthorizedOrder("bob", "CMD-1042"))
        .thenThrow(new SecurityException("Access denied"));

    assertThrows(SecurityException.class, () -> tools.getOrder("CMD-1042", context));
  }

  @Test
  void shouldForwardTrustedIdentityToEligibilityOperation() {
    var context = new ToolContext(Map.of("authenticatedUserId", "alice", "requestId", "request-3"));
    var expected =
        new RefundToolDtos.EligibilityView("CMD-1042", true, "Eligible under policy 1.0");
    when(business.checkEligibility("alice", "CMD-1042")).thenReturn(expected);

    var result = tools.checkRefundEligibility("CMD-1042", context);

    assertEquals(expected, result);
    assertEquals(List.of("checkRefundEligibility"), tracker.drain("request-3"));
    verify(business).checkEligibility("alice", "CMD-1042");
  }
}
