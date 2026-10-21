package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.ai.workflows.RefundOrchestrationFacade;
import com.rickenbazolo.refund.application.ForbiddenOrderAccessException;
import java.util.NoSuchElementException;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/orchestration")
public class RefundOrchestrationController {

  private final RefundOrchestrationFacade facade;
  private final DemoActorResolver actorResolver;

  public RefundOrchestrationController(
      RefundOrchestrationFacade facade, DemoActorResolver actorResolver) {
    this.facade = facade;
    this.actorResolver = actorResolver;
  }

  @PostMapping("/execute")
  public Object execute(
      @RequestHeader("X-Demo-User") String userId, @RequestBody ExecuteRequest request) {
    if (request == null) {
      throw new IllegalArgumentException("Request body must not be null");
    }
    var actor = actorResolver.fromDemoHeader(userId);
    return facade.execute(
        actor.subjectId(), request.mode(), request.message(), request.confirmed());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorReply invalidRequest(IllegalArgumentException exception) {
    return new ErrorReply(exception.getMessage());
  }

  @ExceptionHandler(SecurityException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorReply forbidden() {
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

  @ExceptionHandler(IllegalStateException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorReply conflict() {
    return new ErrorReply("The requested operation could not be completed.");
  }

  @ExceptionHandler(ToolExecutionException.class)
  public ResponseEntity<ErrorReply> toolFailure(ToolExecutionException exception) {
    Throwable cause = exception.getCause();
    if (cause instanceof ForbiddenOrderAccessException || cause instanceof SecurityException) {
      return ResponseEntity.status(HttpStatus.FORBIDDEN)
          .body(new ErrorReply("The requested operation is not authorized."));
    }
    if (cause instanceof NoSuchElementException) {
      return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorReply("Order not found."));
    }
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(new ErrorReply("Tool execution failed."));
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorReply> identityFailure(ResponseStatusException exception) {
    return ResponseEntity.status(exception.getStatusCode())
        .body(new ErrorReply(exception.getReason()));
  }

  public record ExecuteRequest(String mode, String message, boolean confirmed) {}

  public record ErrorReply(String error) {}
}
