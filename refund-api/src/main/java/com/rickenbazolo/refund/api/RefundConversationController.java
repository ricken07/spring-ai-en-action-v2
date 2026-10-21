package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.ai.chat.ConversationSnapshot;
import com.rickenbazolo.refund.ai.chat.RefundConversationService;
import com.rickenbazolo.refund.api.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/conversations")
public class RefundConversationController {

  private final RefundConversationService service;
  private final CurrentUser currentUser;

  public RefundConversationController(RefundConversationService service, CurrentUser currentUser) {
    this.service = service;
    this.currentUser = currentUser;
  }

  @PostMapping("/{conversationId}/messages")
  public ChatReply send(
      @PathVariable("conversationId") String conversationId, @RequestBody ChatRequest request) {
    String answer = service.reply(currentUser.id(), conversationId, request.message());
    return new ChatReply(conversationId, answer);
  }

  @GetMapping("/{conversationId}/memory")
  public ConversationSnapshot inspect(@PathVariable("conversationId") String conversationId) {
    return service.inspect(currentUser.id(), conversationId);
  }

  @DeleteMapping("/{conversationId}/memory")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clear(@PathVariable("conversationId") String conversationId) {
    service.clear(currentUser.id(), conversationId);
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
