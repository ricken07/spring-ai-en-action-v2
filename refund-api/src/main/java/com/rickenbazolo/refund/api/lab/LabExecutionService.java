package com.rickenbazolo.refund.api.lab;

import com.rickenbazolo.refund.ai.chat.RefundAiService;
import com.rickenbazolo.refund.ai.chat.RefundConversationService;
import com.rickenbazolo.refund.ai.mcp.RefundMcpChatService;
import com.rickenbazolo.refund.ai.mcp.RefundMcpChatService.McpChatResult;
import com.rickenbazolo.refund.ai.rag.RagAnswer;
import com.rickenbazolo.refund.ai.rag.RefundRagService;
import com.rickenbazolo.refund.ai.rag.advanced.AdvancedRagAnswer;
import com.rickenbazolo.refund.ai.rag.advanced.AdvancedRagPipeline;
import com.rickenbazolo.refund.ai.tools.RefundToolChatService;
import com.rickenbazolo.refund.ai.tools.RefundToolChatService.ToolChatResult;
import com.rickenbazolo.refund.ai.workflows.RefundAgentResult;
import com.rickenbazolo.refund.ai.workflows.RefundOrchestrationFacade;
import com.rickenbazolo.refund.ai.workflows.RefundWorkflowResult;
import com.rickenbazolo.refund.api.observability.AiExecutionMetrics;
import com.rickenbazolo.refund.api.reliability.AiExecutionGuard;
import com.rickenbazolo.refund.api.reliability.SafeAiResponse;
import com.rickenbazolo.refund.api.reliability.SafeRefundAiService;
import com.rickenbazolo.refund.application.ForbiddenOrderAccessException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class LabExecutionService {

  private final LabScenarioCatalog catalog;
  private final LabEvidenceStore store;
  private final RefundAiService chat;
  private final RefundConversationService memory;
  private final RefundRagService rag;
  private final AdvancedRagPipeline advancedRag;
  private final RefundToolChatService tools;
  private final RefundMcpChatService mcp;
  private final RefundOrchestrationFacade orchestration;
  private final SafeRefundAiService safeChat;
  private final AiExecutionGuard guard;
  private final AiExecutionMetrics metrics;

  public LabExecutionService(
      LabScenarioCatalog catalog,
      LabEvidenceStore store,
      RefundAiService chat,
      RefundConversationService memory,
      RefundRagService rag,
      AdvancedRagPipeline advancedRag,
      RefundToolChatService tools,
      RefundMcpChatService mcp,
      RefundOrchestrationFacade orchestration,
      SafeRefundAiService safeChat,
      AiExecutionGuard guard,
      AiExecutionMetrics metrics) {
    this.catalog = catalog;
    this.store = store;
    this.chat = chat;
    this.memory = memory;
    this.rag = rag;
    this.advancedRag = advancedRag;
    this.tools = tools;
    this.mcp = mcp;
    this.orchestration = orchestration;
    this.safeChat = safeChat;
    this.guard = guard;
    this.metrics = metrics;
  }

  public LabExecutionResult execute(String authenticatedUserId, LabExecutionRequest request) {
    if (authenticatedUserId == null || authenticatedUserId.isBlank()) {
      throw new SecurityException("Authenticated user required");
    }
    if (request == null || request.question() == null || request.question().isBlank()) {
      throw new IllegalArgumentException("Question must not be blank");
    }

    LabScenario scenario = catalog.require(request.scenarioId());
    if ("WORKFLOW".equals(scenario.id()) && request.confirmed()) {
      throw new IllegalArgumentException(
          "Draft creation is unavailable from the generic lab runner");
    }
    if ("AGENT".equals(scenario.id()) && request.confirmed()) {
      throw new IllegalArgumentException("Agent mode does not support draft confirmation");
    }
    String conversationId = request.conversationId();
    if ("MEMORY".equals(scenario.id()) && (conversationId == null || conversationId.isBlank())) {
      throw new IllegalArgumentException("Conversation ID required");
    }

    String executionId = UUID.randomUUID().toString();
    var recorder = new LabEvidenceRecorder();
    try {
      Object output =
          metrics.measure(
              scenario.id(),
              () ->
                  recorder.record(
                      "APPLICATION",
                      "RUN_" + scenario.id(),
                      () -> executeReadOnly(authenticatedUserId, request)));

      var result =
          new LabExecutionResult(
              executionId,
              scenario.id(),
              "SUCCESS",
              output,
              recorder.snapshot(),
              diagnostics(output));
      store.save(authenticatedUserId, result);
      return result;
    } catch (RuntimeException exception) {
      var result =
          new LabExecutionResult(
              executionId,
              scenario.id(),
              "ERROR",
              null,
              recorder.snapshot(),
              Map.of("errorType", exception.getClass().getSimpleName()));
      store.save(authenticatedUserId, result);
      if (exception instanceof ForbiddenOrderAccessException) {
        throw new OrderAccessDeniedException();
      }
      return result;
    } catch (Exception exception) {
      var result =
          new LabExecutionResult(
              executionId,
              scenario.id(),
              "ERROR",
              null,
              recorder.snapshot(),
              Map.of("errorType", "ExecutionFailure"));
      store.save(authenticatedUserId, result);
      return result;
    }
  }

  private Object executeReadOnly(String userId, LabExecutionRequest request) {
    return switch (request.scenarioId()) {
      case "CHAT" -> guard.execute(() -> chat.chat(request.question()));
      case "STRUCTURED" -> guard.execute(() -> chat.analyze(request.question()));
      case "MEMORY" -> {
        String answer = memory.reply(userId, request.conversationId(), request.question());
        yield new MemoryOutput(answer, userId, memory.inspect(userId, request.conversationId()));
      }
      case "RAG" -> guard.execute(() -> rag.ask(request.question()));
      case "RAG_ADVANCED" ->
          guard.execute(() -> advancedRag.ask(request.question(), request.reranking()));
      case "TOOLS" -> guard.execute(() -> tools.ask(userId, request.question()));
      case "MCP" -> guard.execute(() -> mcp.ask(request.question()));
      case "WORKFLOW" ->
          guard.execute(() -> orchestration.execute(userId, "WORKFLOW", request.question(), false));
      case "AGENT" ->
          guard.execute(() -> orchestration.execute(userId, "AGENT", request.question(), false));
      case "SAFE_CHAT" -> safeChat.ask(request.question());
      default -> throw new IllegalArgumentException("Unsupported lab scenario");
    };
  }

  private Map<String, Object> diagnostics(Object output) {
    var diagnostics = new LinkedHashMap<String, Object>();
    diagnostics.put(
        "evidenceScope",
        "Frontière du scénario ; détails internes uniquement lorsque le service les émet");
    if (output instanceof ToolChatResult result) {
      diagnostics.put("toolInvocations", result.invokedTools());
    } else if (output instanceof McpChatResult result) {
      diagnostics.put("toolInvocations", result.invokedTools());
    } else if (output instanceof RefundAgentResult result) {
      diagnostics.put("toolInvocations", result.invokedTools());
    } else if (output instanceof RefundWorkflowResult result) {
      diagnostics.put("workflowSteps", result.executedSteps());
      diagnostics.put("businessStatus", result.status());
    } else if (output instanceof RagAnswer result) {
      diagnostics.put(
          "retrievedPolicyIds",
          result.retrievedPolicies().stream().map(RagAnswer.RetrievedPolicy::policyId).toList());
      diagnostics.put(
          "referenceSemantics",
          "Inspection search; the QuestionAnswerAdvisor performs its own retrieval");
    } else if (output instanceof AdvancedRagAnswer result) {
      diagnostics.put(
          "retrievedPolicyIds",
          result.documents().stream().map(AdvancedRagAnswer.DocumentTrace::policyId).toList());
      diagnostics.put("timing", result.timing());
      diagnostics.put(
          "referenceSemantics",
          "Selected documents are the context passed to the answer generator");
    } else if (output instanceof SafeAiResponse result) {
      diagnostics.put("responseStatus", result.status());
      diagnostics.put("generatedByModel", result.generatedByModel());
    } else if (output instanceof MemoryOutput result) {
      diagnostics.put("storedMessages", result.snapshot().storedMessages());
      diagnostics.put("messageTypes", result.snapshot().messageTypes());
    }
    return Map.copyOf(diagnostics);
  }

  public void clearConversation(String userId, String conversationId) {
    memory.clear(userId, conversationId);
  }

  public record MemoryOutput(
      String answer,
      String userId,
      com.rickenbazolo.refund.ai.chat.ConversationSnapshot snapshot) {}

  public static final class OrderAccessDeniedException extends RuntimeException {}
}
