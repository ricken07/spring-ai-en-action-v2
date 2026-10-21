package com.rickenbazolo.refund.ai.workflows;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.rickenbazolo.refund.ai.chat.RefundChatGateway;
import com.rickenbazolo.refund.ai.chat.RefundIntentValidator;
import com.rickenbazolo.refund.ai.tools.RefundBusinessOperations;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.DraftView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.EligibilityView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.OrderView;
import com.rickenbazolo.refund.domain.RefundIntent;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class RefundWorkflowServiceTest {

  private RefundChatGateway gateway;
  private RefundBusinessOperations business;
  private RefundWorkflowService workflow;

  @BeforeEach
  void setUp() {
    gateway = mock(RefundChatGateway.class);
    business = mock(RefundBusinessOperations.class);
    workflow = new RefundWorkflowService(gateway, new RefundIntentValidator(), business);
  }

  @Test
  void shouldStopAndRequestClarificationForIncompleteIntent() {
    when(gateway.extractIntent(anyString()))
        .thenReturn(new RefundIntent("", "Defective computer", "REFUND", List.of("orderId")));

    var result = workflow.execute("alice", new RefundWorkflowRequest("My computer broke", true));

    assertEquals("NEEDS_CLARIFICATION", result.status());
    assertEquals(List.of("EXTRACT_INTENT", "VALIDATION_FAILED"), result.executedSteps());
    verifyNoInteractions(business);
  }

  @Test
  void shouldStopWhenIntentIsNotARefund() {
    when(gateway.extractIntent(anyString()))
        .thenReturn(new RefundIntent("CMD-1042", "Order status", "INFORMATION", List.of()));

    var result = workflow.execute("alice", new RefundWorkflowRequest("Check my order", true));

    assertEquals("UNSUPPORTED_INTENT", result.status());
    verifyNoInteractions(business);
  }

  @Test
  void shouldStopWhenNotEligibleWithoutCreatingDraft() {
    givenValidIntentAndOrder();
    when(business.checkEligibility("alice", "CMD-1042"))
        .thenReturn(new EligibilityView("CMD-1042", false, "Outside policy"));

    var result = workflow.execute("alice", new RefundWorkflowRequest("Refund CMD-1042", true));

    assertEquals("NOT_ELIGIBLE", result.status());
    verify(business, never()).createDraft(anyString(), anyString());
  }

  @Test
  void shouldRequireConfirmationWithoutCreatingDraft() {
    givenEligibleOrder();

    var result = workflow.execute("alice", new RefundWorkflowRequest("Refund CMD-1042", false));

    assertEquals("CONFIRMATION_REQUIRED", result.status());
    assertEquals(
        List.of(
            "EXTRACT_INTENT",
            "VALIDATE_INTENT",
            "GET_AUTHORIZED_ORDER",
            "CHECK_ELIGIBILITY",
            "AWAIT_CONFIRMATION"),
        result.executedSteps());
    verify(business, never()).createDraft(anyString(), anyString());
  }

  @Test
  void shouldRunDeterministicStepsAndCreateDraftAfterConfirmation() {
    givenEligibleOrder();
    when(business.createDraft("alice", "CMD-1042"))
        .thenReturn(new DraftView("DRAFT-001", "CMD-1042", "DRAFT"));

    var result = workflow.execute("alice", new RefundWorkflowRequest("Refund CMD-1042", true));

    assertEquals("DRAFT_CREATED", result.status());
    assertEquals("DRAFT-001", result.draft().draftId());
    assertEquals(
        List.of(
            "EXTRACT_INTENT",
            "VALIDATE_INTENT",
            "GET_AUTHORIZED_ORDER",
            "CHECK_ELIGIBILITY",
            "CREATE_DRAFT"),
        result.executedSteps());
    InOrder order = inOrder(gateway, business);
    order.verify(gateway).extractIntent("Refund CMD-1042");
    order.verify(business).getAuthorizedOrder("alice", "CMD-1042");
    order.verify(business).checkEligibility("alice", "CMD-1042");
    order.verify(business).createDraft("alice", "CMD-1042");
  }

  @Test
  void shouldRejectMissingUserOrMessageBeforeCallingDependencies() {
    assertThrows(
        SecurityException.class,
        () -> workflow.execute(" ", new RefundWorkflowRequest("Refund", false)));
    assertThrows(
        IllegalArgumentException.class,
        () -> workflow.execute("alice", new RefundWorkflowRequest(" ", false)));
    verifyNoInteractions(gateway, business);
  }

  private void givenValidIntentAndOrder() {
    when(gateway.extractIntent(anyString()))
        .thenReturn(new RefundIntent("CMD-1042", "Defective computer", "REFUND", List.of()));
    when(business.getAuthorizedOrder("alice", "CMD-1042"))
        .thenReturn(new OrderView("CMD-1042", "DELIVERED", "Computer", "2026-10-10"));
  }

  private void givenEligibleOrder() {
    givenValidIntentAndOrder();
    when(business.checkEligibility("alice", "CMD-1042"))
        .thenReturn(new EligibilityView("CMD-1042", true, "Eligible"));
  }
}
