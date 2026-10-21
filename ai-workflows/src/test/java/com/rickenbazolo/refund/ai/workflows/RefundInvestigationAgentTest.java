package com.rickenbazolo.refund.ai.workflows;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.rickenbazolo.refund.ai.tools.RefundBusinessOperations;
import com.rickenbazolo.refund.ai.tools.RefundReadTools;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.EligibilityView;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.OrderView;
import com.rickenbazolo.refund.ai.tools.ToolInvocationTracker;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;

class RefundInvestigationAgentTest {

  @Test
  void shouldRejectBlankQuestionAndMissingIdentityBeforeCallingModel() {
    ChatClient chatClient = mock(ChatClient.class);
    var tracker = new ToolInvocationTracker();
    RefundReadTools tools = new RefundReadTools(mock(RefundBusinessOperations.class), tracker);
    var agent = new RefundInvestigationAgent(chatClient, tools, tracker);

    assertThrows(IllegalArgumentException.class, () -> agent.investigate("alice", " "));
    assertThrows(SecurityException.class, () -> agent.investigate("", "Check CMD-1042"));
    verifyNoInteractions(chatClient);
  }

  @Test
  void shouldRunMultiCallSpringAiLoopWithOnlyReadTools() {
    ChatModel model = mock(ChatModel.class);
    RefundBusinessOperations business = mock(RefundBusinessOperations.class);
    var tracker = new ToolInvocationTracker();
    var tools = new RefundReadTools(business, tracker);
    when(business.getAuthorizedOrder("alice", "CMD-1042"))
        .thenReturn(new OrderView("CMD-1042", "DELIVERED", "Computer", "2026-10-10"));
    when(business.checkEligibility("alice", "CMD-1042"))
        .thenReturn(new EligibilityView("CMD-1042", true, "Eligible under policy 1.0"));
    when(model.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());

    AtomicInteger modelCalls = new AtomicInteger();
    when(model.call(any(Prompt.class)))
        .thenAnswer(
            invocation -> {
              int call = modelCalls.getAndIncrement();
              if (call < 2) {
                String toolName = call == 0 ? "getOrder" : "checkRefundEligibility";
                var toolCall =
                    new AssistantMessage.ToolCall(
                        "call-" + call, "function", toolName, "{\"orderId\":\"CMD-1042\"}");
                var toolRequest =
                    AssistantMessage.builder().content("").toolCalls(List.of(toolCall)).build();
                return new ChatResponse(List.of(new Generation(toolRequest)));
              }
              return new ChatResponse(
                  List.of(new Generation(new AssistantMessage("La commande est éligible."))));
            });

    var agent = new RefundInvestigationAgent(ChatClient.create(model), tools, tracker);
    var result = agent.investigate("alice", "Analyse CMD-1042 et vérifie son éligibilité.");

    assertEquals("La commande est éligible.", result.answer());
    assertEquals("AGENTIC_LOOP", result.executionMode());
    assertEquals(List.of("getOrder", "checkRefundEligibility"), result.invokedTools());
    assertEquals(3, modelCalls.get());
    verify(business).getAuthorizedOrder("alice", "CMD-1042");
    verify(business).checkEligibility("alice", "CMD-1042");
    verify(business, never()).createDraft(any(), any());
  }

  @Test
  void shouldRespectBusinessAuthorizationWhenToolIsCalled() {
    ChatModel model = mock(ChatModel.class);
    RefundBusinessOperations business = mock(RefundBusinessOperations.class);
    var tracker = new ToolInvocationTracker();
    var tools = new RefundReadTools(business, tracker);
    when(business.getAuthorizedOrder("bob", "CMD-1042"))
        .thenThrow(new com.rickenbazolo.refund.application.ForbiddenOrderAccessException());
    when(model.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());
    AtomicInteger modelCalls = new AtomicInteger();
    when(model.call(any(Prompt.class)))
        .thenAnswer(
            invocation -> {
              if (modelCalls.getAndIncrement() == 0) {
                var toolCall =
                    new AssistantMessage.ToolCall(
                        "call-denied", "function", "getOrder", "{\"orderId\":\"CMD-1042\"}");
                return new ChatResponse(
                    List.of(
                        new Generation(
                            AssistantMessage.builder()
                                .content("")
                                .toolCalls(List.of(toolCall))
                                .build())));
              }
              return new ChatResponse(
                  List.of(
                      new Generation(
                          new AssistantMessage("Je ne peux pas consulter cette commande."))));
            });

    var agent = new RefundInvestigationAgent(ChatClient.create(model), tools, tracker);
    var result = agent.investigate("bob", "Consulte CMD-1042");

    assertEquals(List.of("getOrder"), result.invokedTools());
    assertEquals(2, modelCalls.get());
    verify(business).getAuthorizedOrder("bob", "CMD-1042");
    verify(business, never()).checkEligibility(any(), any());
    verify(business, never()).createDraft(any(), any());
  }
}
