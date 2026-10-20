package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.ai.chat.ConversationSnapshot;
import com.rickenbazolo.refund.ai.chat.RefundConversationService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/conversations")
public class RefundConversationController {

  private final RefundConversationService service;

  public RefundConversationController(RefundConversationService service) {
    this.service = service;
  }

  @PostMapping("/{conversationId}/messages")
  public ChatReply send(
      @RequestHeader("X-Demo-User") String userId,
      @PathVariable("conversationId") String conversationId,
      @RequestBody ChatRequest request) {
    String answer = service.reply(userId, conversationId, request.message());
    return new ChatReply(conversationId, answer);
  }

  @GetMapping("/{conversationId}/memory")
  public ConversationSnapshot inspect(
      @RequestHeader("X-Demo-User") String userId,
      @PathVariable("conversationId") String conversationId) {
    return service.inspect(userId, conversationId);
  }

  @DeleteMapping("/{conversationId}/memory")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clear(
      @RequestHeader("X-Demo-User") String userId,
      @PathVariable("conversationId") String conversationId) {
    service.clear(userId, conversationId);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorReply invalidInput(IllegalArgumentException exception) {
    return new ErrorReply(exception.getMessage());
  }

  public record ChatRequest(String message) {}

  public record ChatReply(String conversationId, String answer) {}

  public record ErrorReply(String error) {}
}
