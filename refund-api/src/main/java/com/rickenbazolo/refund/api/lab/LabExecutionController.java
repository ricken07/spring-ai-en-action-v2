package com.rickenbazolo.refund.api.lab;

import com.rickenbazolo.refund.api.security.CurrentUser;
import java.util.List;
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
@RequestMapping("/api/lab")
public class LabExecutionController {

  private final LabScenarioCatalog catalog;
  private final LabExecutionService service;
  private final CurrentUser currentUser;

  public LabExecutionController(
      LabScenarioCatalog catalog, LabExecutionService service, CurrentUser currentUser) {
    this.catalog = catalog;
    this.service = service;
    this.currentUser = currentUser;
  }

  @GetMapping("/scenarios")
  public List<LabScenario> scenarios() {
    return catalog.all();
  }

  @PostMapping("/executions")
  public LabExecutionResult execute(@RequestBody LabExecutionRequest request) {
    return service.execute(currentUser.id(), request);
  }

  @DeleteMapping("/conversations/{conversationId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void clearConversation(@PathVariable String conversationId) {
    service.clearConversation(currentUser.id(), conversationId);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse invalidInput(IllegalArgumentException exception) {
    return new ErrorResponse(exception.getMessage());
  }

  @ExceptionHandler(SecurityException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorResponse forbidden() {
    return new ErrorResponse("The requested operation is not authorized.");
  }

  @ExceptionHandler(LabExecutionService.OrderAccessDeniedException.class)
  @ResponseStatus(HttpStatus.FORBIDDEN)
  public ErrorResponse orderAccessDenied() {
    return new ErrorResponse("The requested operation is not authorized.");
  }

  public record ErrorResponse(String error) {}
}
