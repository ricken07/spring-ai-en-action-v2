package com.rickenbazolo.refund.api.lab;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.rickenbazolo.refund.ai.chat.RefundAiService;
import com.rickenbazolo.refund.ai.chat.RefundConversationService;
import com.rickenbazolo.refund.ai.mcp.RefundMcpChatService;
import com.rickenbazolo.refund.ai.rag.RefundRagService;
import com.rickenbazolo.refund.ai.rag.advanced.AdvancedRagAnswer;
import com.rickenbazolo.refund.ai.rag.advanced.AdvancedRagPipeline;
import com.rickenbazolo.refund.ai.tools.RefundToolChatService;
import com.rickenbazolo.refund.ai.workflows.RefundOrchestrationFacade;
import com.rickenbazolo.refund.api.observability.AiExecutionMetrics;
import com.rickenbazolo.refund.api.reliability.AiExecutionGuard;
import com.rickenbazolo.refund.api.reliability.SafeRefundAiService;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class LabExecutionServiceTest {

  @Test
  void shouldDispatchChatAndRecordScenarioBoundary() throws Exception {
    var chat = mock(RefundAiService.class);
    when(chat.chat("Explain refund policy")).thenReturn("Contact support for review.");
    var store = new LabEvidenceStore();
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var service = service(chat, mock(AdvancedRagPipeline.class), store, executor);

      var result =
          service.execute(
              "alice",
              new LabExecutionRequest("CHAT", "Explain refund policy", null, false, false));

      assertEquals("SUCCESS", result.status());
      assertEquals("Contact support for review.", result.output());
      assertEquals("RUN_CHAT", result.evidence().getFirst().operation());
      assertEquals("SUCCESS", result.evidence().getFirst().outcome());
      assertTrue(store.find("alice", result.executionId()).isPresent());
      assertTrue(store.find("bob", result.executionId()).isEmpty());
      verify(chat).chat("Explain refund policy");
    }
  }

  @Test
  void shouldForwardRerankingChoice() throws Exception {
    var advancedRag = mock(AdvancedRagPipeline.class);
    var answer =
        new AdvancedRagAnswer(
            "q", "answer", java.util.List.of(), new AdvancedRagAnswer.Timing(1, 0, 1, 2));
    when(advancedRag.ask("q", true)).thenReturn(answer);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var result =
          service(mock(RefundAiService.class), advancedRag, new LabEvidenceStore(), executor)
              .execute("alice", new LabExecutionRequest("RAG_ADVANCED", "q", null, true, false));

      assertEquals(answer, result.output());
      assertEquals(0, ((java.util.List<?>) result.diagnostics().get("retrievedPolicyIds")).size());
      verify(advancedRag).ask("q", true);
    }
  }

  @Test
  void shouldRecordActualToolsReportedByService() throws Exception {
    var tools = mock(RefundToolChatService.class);
    when(tools.ask("alice", "Inspect order"))
        .thenReturn(
            new RefundToolChatService.ToolChatResult(
                "r1", "Order is delivered", java.util.List.of("getOrder")));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var result =
          service(
                  mock(RefundAiService.class),
                  mock(AdvancedRagPipeline.class),
                  new LabEvidenceStore(),
                  executor,
                  tools)
              .execute(
                  "alice", new LabExecutionRequest("TOOLS", "Inspect order", null, false, false));

      assertEquals(java.util.List.of("getOrder"), result.diagnostics().get("toolInvocations"));
      assertEquals("SUCCESS", result.evidence().getFirst().outcome());
    }
  }

  @Test
  void shouldNotAllowDraftWriteFromGenericRunner() {
    var orchestration = mock(RefundOrchestrationFacade.class);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var service =
          service(
              mock(RefundAiService.class),
              mock(AdvancedRagPipeline.class),
              new LabEvidenceStore(),
              executor,
              mock(RefundToolChatService.class),
              orchestration);

      assertThrows(
          IllegalArgumentException.class,
          () ->
              service.execute(
                  "alice", new LabExecutionRequest("WORKFLOW", "q", null, false, true)));
      verifyNoInteractions(orchestration);
    }
  }

  @Test
  void shouldReturnSanitizedFailureEvidence() throws Exception {
    var chat = mock(RefundAiService.class);
    when(chat.chat(anyString())).thenThrow(new IllegalStateException("secret provider detail"));
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      var result =
          service(chat, mock(AdvancedRagPipeline.class), new LabEvidenceStore(), executor)
              .execute("alice", new LabExecutionRequest("CHAT", "q", null, false, false));

      assertEquals("ERROR", result.status());
      assertEquals("IllegalStateException", result.diagnostics().get("errorType"));
      assertFalse(result.diagnostics().containsKey("error"));
      assertEquals("ERROR", result.evidence().getFirst().outcome());
    }
  }

  private LabExecutionService service(
      RefundAiService chat,
      AdvancedRagPipeline advancedRag,
      LabEvidenceStore store,
      java.util.concurrent.ExecutorService executor) {
    return service(chat, advancedRag, store, executor, mock(RefundToolChatService.class));
  }

  private LabExecutionService service(
      RefundAiService chat,
      AdvancedRagPipeline advancedRag,
      LabEvidenceStore store,
      java.util.concurrent.ExecutorService executor,
      RefundToolChatService tools) {
    return service(
        chat, advancedRag, store, executor, tools, mock(RefundOrchestrationFacade.class));
  }

  private LabExecutionService service(
      RefundAiService chat,
      AdvancedRagPipeline advancedRag,
      LabEvidenceStore store,
      java.util.concurrent.ExecutorService executor,
      RefundToolChatService tools,
      RefundOrchestrationFacade orchestration) {
    return new LabExecutionService(
        new LabScenarioCatalog(),
        store,
        chat,
        mock(RefundConversationService.class),
        mock(RefundRagService.class),
        advancedRag,
        tools,
        mock(RefundMcpChatService.class),
        orchestration,
        mock(SafeRefundAiService.class),
        new AiExecutionGuard(executor, Duration.ofSeconds(2)),
        new AiExecutionMetrics(new SimpleMeterRegistry()));
  }
}
