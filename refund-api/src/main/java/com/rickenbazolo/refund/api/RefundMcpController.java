package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.ai.mcp.RefundMcpChatService;
import com.rickenbazolo.refund.ai.mcp.RefundMcpChatService.McpChatResult;
import com.rickenbazolo.refund.ai.mcp.RefundMcpToolCatalog;
import java.util.List;
import org.springframework.ai.tool.execution.ToolExecutionException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/mcp")
public class RefundMcpController {

  private final RefundMcpChatService chat;
  private final RefundMcpToolCatalog catalog;

  public RefundMcpController(RefundMcpChatService chat, RefundMcpToolCatalog catalog) {
    this.chat = chat;
    this.catalog = catalog;
  }

  @GetMapping("/tools")
  public List<String> tools() {
    return catalog.names();
  }

  @PostMapping("/ask")
  public McpChatResult ask(@RequestBody ChatRequest request) {
    return chat.ask(request.message());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorReply invalidInput(IllegalArgumentException exception) {
    return new ErrorReply(exception.getMessage());
  }

  @ExceptionHandler(ToolExecutionException.class)
  @ResponseStatus(HttpStatus.BAD_GATEWAY)
  public ErrorReply mcpFailure(ToolExecutionException exception) {
    return new ErrorReply("The remote refund policy service could not complete the request.");
  }

  public record ChatRequest(String message) {}

  public record ErrorReply(String error) {}
}
