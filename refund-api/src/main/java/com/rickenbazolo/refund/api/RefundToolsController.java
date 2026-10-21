package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.ai.tools.RefundDraftCommandService;
import com.rickenbazolo.refund.ai.tools.RefundToolChatService;
import com.rickenbazolo.refund.ai.tools.RefundToolChatService.ToolChatResult;
import com.rickenbazolo.refund.ai.tools.RefundToolDtos.DraftView;
import com.rickenbazolo.refund.api.security.CurrentUser;
import com.rickenbazolo.refund.application.ForbiddenOrderAccessException;
import java.util.NoSuchElementException;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/tools")
public class RefundToolsController {

  private final RefundToolChatService chatService;
  private final RefundDraftCommandService draftService;
  private final CurrentUser currentUser;

  public RefundToolsController(
      RefundToolChatService chatService,
      RefundDraftCommandService draftService,
      CurrentUser currentUser) {
    this.chatService = chatService;
    this.draftService = draftService;
    this.currentUser = currentUser;
  }

  @PostMapping("/ask")
  public ToolChatResult ask(@RequestBody ChatRequest request) {
    return chatService.ask(currentUser.id(), request.message());
  }

  @PostMapping("/drafts")
  public DraftView prepareDraft(@RequestBody DraftRequest request) {
    return draftService.prepareDraft(currentUser.id(), request.orderId(), request.confirmed());
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

  public record ChatRequest(String message) {}

  public record DraftRequest(String orderId, boolean confirmed) {}

  public record ErrorReply(String error) {}
}
