package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.ai.tools.RefundDraftCommandService;
import com.rickenbazolo.refund.ai.tools.RefundToolChatService;
import com.rickenbazolo.refund.ai.tools.RefundToolChatService.ToolChatResult;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.DraftView;
import com.rickenbazolo.refund.application.ForbiddenOrderAccessException;
import java.util.NoSuchElementException;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/tools")
public class RefundToolsController {

  private final RefundToolChatService chatService;
  private final RefundDraftCommandService draftService;
  private final DemoActorResolver actorResolver;

  public RefundToolsController(
      RefundToolChatService chatService,
      RefundDraftCommandService draftService,
      DemoActorResolver actorResolver) {
    this.chatService = chatService;
    this.draftService = draftService;
    this.actorResolver = actorResolver;
  }

  @PostMapping("/ask")
  public ToolChatResult ask(
      @RequestHeader("X-Demo-User") String userId, @RequestBody ChatRequest request) {
    var actor = actorResolver.fromDemoHeader(userId);
    return chatService.ask(actor.subjectId(), request.message());
  }

  @PostMapping("/drafts")
  public DraftView prepareDraft(
      @RequestHeader("X-Demo-User") String userId, @RequestBody DraftRequest request) {
    var actor = actorResolver.fromDemoHeader(userId);
    return draftService.prepareDraft(actor.subjectId(), request.orderId(), request.confirmed());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorReply invalidInput(IllegalArgumentException exception) {
    return new ErrorReply(exception.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorReply conflict(IllegalStateException exception) {
    return new ErrorReply(exception.getMessage());
  }

  @ExceptionHandler(SecurityException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorReply forbidden(SecurityException exception) {
    return new ErrorReply("The requested operation is not authorized.");
  }

  @ExceptionHandler(ForbiddenOrderAccessException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorReply forbiddenOrder() {
    return new ErrorReply("The requested operation is not authorized.");
  }

  @ExceptionHandler(NoSuchElementException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorReply notFound() {
    return new ErrorReply("Order not found.");
  }

  @ExceptionHandler(ToolExecutionException.class)
  public org.springframework.http.ResponseEntity<ErrorReply> toolFailure(
      ToolExecutionException exception) {
    Throwable cause = exception.getCause();
    if (cause instanceof ForbiddenOrderAccessException || cause instanceof SecurityException) {
      return org.springframework.http.ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new ErrorReply("The requested operation is not authorized."));
    }
    if (cause instanceof NoSuchElementException) {
      return org.springframework.http.ResponseEntity.status(HttpStatus.NOT_FOUND)
          .body(new ErrorReply("Order not found."));
    }
    if (cause instanceof IllegalStateException) {
      return org.springframework.http.ResponseEntity.status(HttpStatus.CONFLICT)
          .body(new ErrorReply("The requested operation could not be completed."));
    }
    return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorReply("Tool execution failed."));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public org.springframework.http.ResponseEntity<ErrorReply> identityFailure(
      ResponseStatusException exception) {
    return org.springframework.http.ResponseEntity.status(exception.getStatusCode())
        .body(new ErrorReply(exception.getReason()));
  }

  public record ChatRequest(String message) {}

  public record DraftRequest(String orderId, boolean confirmed) {}

  public record ErrorReply(String error) {}
}
