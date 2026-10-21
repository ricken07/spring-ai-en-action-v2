package com.rickenbazolo.refund.ai.workflows;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;

class RefundOrchestrationFacadeTest {

  @Test
  void shouldRouteToAgent() {
    RefundWorkflowService workflow = mock(RefundWorkflowService.class);
    RefundInvestigationAgent agent = mock(RefundInvestigationAgent.class);
    var facade = new RefundOrchestrationFacade(workflow, agent);

    facade.execute("alice", "AGENT", "Check CMD-1042", false);

    verify(agent).investigate("alice", "Check CMD-1042");
    verifyNoInteractions(workflow);
  }

  @Test
  void shouldRouteToWorkflow() {
    RefundWorkflowService workflow = mock(RefundWorkflowService.class);
    RefundInvestigationAgent agent = mock(RefundInvestigationAgent.class);
    var facade = new RefundOrchestrationFacade(workflow, agent);

    facade.execute("alice", "WORKFLOW", "Refund CMD-1042", true);

    verify(workflow).execute("alice", new RefundWorkflowRequest("Refund CMD-1042", true));
    verifyNoInteractions(agent);
  }

  @Test
  void shouldRejectAgentWriteConfirmation() {
    RefundWorkflowService workflow = mock(RefundWorkflowService.class);
    RefundInvestigationAgent agent = mock(RefundInvestigationAgent.class);
    var facade = new RefundOrchestrationFacade(workflow, agent);

    assertThrows(
        IllegalArgumentException.class,
        () -> facade.execute("alice", "AGENT", "Create a draft", true));
    verifyNoInteractions(workflow, agent);
  }

  @Test
  void shouldRejectUnknownOrMissingMode() {
    var facade =
        new RefundOrchestrationFacade(
            mock(RefundWorkflowService.class), mock(RefundInvestigationAgent.class));

    assertThrows(
        IllegalArgumentException.class, () -> facade.execute("alice", "UNKNOWN", "message", false));
    assertThrows(
        IllegalArgumentException.class, () -> facade.execute("alice", null, "message", false));
  }
}
