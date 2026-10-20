package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.ai.rag.RagAnswer;
import com.rickenbazolo.refund.ai.rag.RefundPolicyIngestionService;
import com.rickenbazolo.refund.ai.rag.RefundRagService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/rag")
public class RefundRagController {

  private final RefundRagService ragService;
  private final RefundPolicyIngestionService ingestionService;

  public RefundRagController(
      RefundRagService ragService, RefundPolicyIngestionService ingestionService) {
    this.ragService = ragService;
    this.ingestionService = ingestionService;
  }

  @PostMapping("/ask")
  public RagAnswer ask(@RequestBody QuestionRequest request) {
    return ragService.ask(request.question());
  }

  @PostMapping("/admin/ingest")
  public Map<String, Integer> ingest() {
    return Map.of("indexedChunks", ingestionService.ingest());
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public Map<String, String> invalidInput(IllegalArgumentException exception) {
    return Map.of("error", exception.getMessage());
  }

  public record QuestionRequest(String question) {}
}
