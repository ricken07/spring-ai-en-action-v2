package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.ai.chat.RefundAiService;
import com.rickenbazolo.refund.api.reliability.SafeAiResponse;
import com.rickenbazolo.refund.api.reliability.SafeRefundAiService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class RefundAiController {

  private final RefundAiService service;
  private final SafeRefundAiService safeService;

  public RefundAiController(RefundAiService service, SafeRefundAiService safeService) {
    this.service = service;
    this.safeService = safeService;
  }

  @PostMapping("/chat")
  public ChatReply chat(@RequestBody ChatRequest request) {
    return new ChatReply(service.chat(request.message()));
  }

  @PostMapping("/analyze")
  public RefundAiService.IntentAnalysis analyze(@RequestBody ChatRequest request) {
    return service.analyze(request.message());
  }

  @PostMapping("/safe-chat")
  public SafeAiResponse safeChat(@RequestBody ChatRequest request) {
    return safeService.ask(request.message());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorReply invalidInput(IllegalArgumentException exception) {
    return new ErrorReply(exception.getMessage());
  }

  public record ChatRequest(String message) {}

  public record ChatReply(String answer) {}

  public record ErrorReply(String error) {}
}
