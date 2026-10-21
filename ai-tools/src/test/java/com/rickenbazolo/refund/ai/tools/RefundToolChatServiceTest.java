package com.rickenbazolo.refund.ai.tools;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.annotation.Tool;

class RefundToolChatServiceTest {

  @Test
  void shouldProvideToolsAndTrustedContextForTheRequest() {
    ChatClient client = mock(ChatClient.class);
    ChatClient.ChatClientRequestSpec request = mock(ChatClient.ChatClientRequestSpec.class);
    ChatClient.CallResponseSpec response = mock(ChatClient.CallResponseSpec.class);
    RefundBusinessOperations business = mock(RefundBusinessOperations.class);
    ToolInvocationTracker tracker = new ToolInvocationTracker();
    RefundReadTools tools = new RefundReadTools(business, tracker);

    when(client.prompt()).thenReturn(request);
    when(request.user("check CMD-1042")).thenReturn(request);
    when(request.tools(tools)).thenReturn(request);
    when(request.toolContext(anyMap())).thenReturn(request);
    when(request.call()).thenReturn(response);
    when(response.content()).thenReturn("eligible");

    var result = new RefundToolChatService(client, tools, tracker).ask("alice", "check CMD-1042");

    assertEquals("eligible", result.answer());
    assertTrue(result.invokedTools().isEmpty());
    var contextCaptor = ArgumentCaptor.forClass(Map.class);
    verify(request).toolContext(contextCaptor.capture());
    assertEquals("alice", contextCaptor.getValue().get("authenticatedUserId"));
    assertNotNull(contextCaptor.getValue().get("requestId"));
    verify(request).tools(tools);
  }

  @Test
  void shouldRejectMissingIdentityOrQuestionBeforeCallingModel() {
    ChatClient client = mock(ChatClient.class);
    var tools =
        new RefundReadTools(mock(RefundBusinessOperations.class), new ToolInvocationTracker());
    var service = new RefundToolChatService(client, tools, new ToolInvocationTracker());

    assertThrows(SecurityException.class, () -> service.ask(" ", "question"));
    assertThrows(IllegalArgumentException.class, () -> service.ask("alice", " "));
    verifyNoInteractions(client);
  }

  @Test
  void shouldExecuteRealSpringAiToolCallingLoopWithMockedModel() {
    ChatModel model = mock(ChatModel.class);
    RefundBusinessOperations business = mock(RefundBusinessOperations.class);
    var tracker = new ToolInvocationTracker();
    var tools = new RefundReadTools(business, tracker);
    var order = new RefundToolDtos.OrderView("CMD-1042", "DELIVERED", "LAPTOP", "2026-10-10");
    when(business.getAuthorizedOrder("alice", "CMD-1042")).thenReturn(order);
    when(business.checkEligibility("alice", "CMD-1042"))
        .thenReturn(
            new RefundToolDtos.EligibilityView("CMD-1042", true, "Eligible under policy 1.0"));
    when(model.getOptions()).thenReturn(ToolCallingChatOptions.builder().build());

    AtomicInteger modelCalls = new AtomicInteger();
    when(model.call(any(Prompt.class)))
        .thenAnswer(
            invocation -> {
              int call = modelCalls.getAndIncrement();
              if (call == 0 || call == 1) {
                String toolName = call == 0 ? "getOrder" : "checkRefundEligibility";
                var toolCall =
                    new AssistantMessage.ToolCall(
                        "call-" + call, "function", toolName, "{\"orderId\":\"CMD-1042\"}");
                var toolRequest =
                    AssistantMessage.builder()
                        .content("")
                        .toolCalls(java.util.List.of(toolCall))
                        .build();
                return new ChatResponse(java.util.List.of(new Generation(toolRequest)));
              }
              return new ChatResponse(
                  java.util.List.of(new Generation(new AssistantMessage("Order is delivered."))));
            });

    var chatClient = ChatClient.create(model);
    var service = new RefundToolChatService(chatClient, tools, tracker);

    var result = service.ask("alice", "Check CMD-1042");

    assertEquals("Order is delivered.", result.answer());
    assertEquals(java.util.List.of("getOrder", "checkRefundEligibility"), result.invokedTools());
    assertEquals(3, modelCalls.get());
    verify(business).getAuthorizedOrder("alice", "CMD-1042");
    verify(business).checkEligibility("alice", "CMD-1042");
  }

  @Test
  void shouldExposeOnlyReadOperationsAsTools() {
    var methods = RefundReadTools.class.getDeclaredMethods();

    assertTrue(
        java.util.Arrays.stream(methods)
            .filter(method -> method.isAnnotationPresent(Tool.class))
            .map(method -> method.getAnnotation(Tool.class).name())
            .allMatch(name -> name.equals("getOrder") || name.equals("checkRefundEligibility")));
    assertEquals(
        2,
        java.util.Arrays.stream(methods)
            .filter(method -> method.isAnnotationPresent(Tool.class))
            .count());
  }
}
