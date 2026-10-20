package com.rickenbazolo.refund.api;

import com.rickenbazolo.refund.ai.rag.advanced.AdvancedRagAnswer;
import com.rickenbazolo.refund.ai.rag.advanced.AdvancedRagPipeline;
import com.rickenbazolo.refund.ai.rag.advanced.ModularRagService;
import com.rickenbazolo.refund.ai.rag.advanced.RetrievalBenchmarkReport;
import com.rickenbazolo.refund.ai.rag.advanced.RetrievalBenchmarkService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/rag/advanced")
public class AdvancedRagController {

  private final AdvancedRagPipeline pipeline;
  private final RetrievalBenchmarkService benchmark;
  private final ModularRagService modularRagService;

  public AdvancedRagController(
      AdvancedRagPipeline pipeline,
      RetrievalBenchmarkService benchmark,
      ModularRagService modularRagService) {
    this.pipeline = pipeline;
    this.benchmark = benchmark;
    this.modularRagService = modularRagService;
  }

  @PostMapping("/ask")
  public AdvancedRagAnswer ask(@RequestBody AdvancedRagRequest request) {
    return pipeline.ask(request.question(), request.reranking());
  }

  @PostMapping("/modular/ask")
  public ModularRagReply askWithModularRag(@RequestBody QuestionRequest request) {
    return new ModularRagReply(modularRagService.ask(request.question()));
  }

  @GetMapping("/evaluation")
  public RetrievalBenchmarkReport evaluate() {
    return benchmark.evaluate();
  }

  @ExceptionHandler(IllegalArgumentException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorReply invalidInput(IllegalArgumentException exception) {
    return new ErrorReply(exception.getMessage());
  }

  public record AdvancedRagRequest(String question, boolean reranking) {}

  public record QuestionRequest(String question) {}

  public record ModularRagReply(String answer) {}

  public record ErrorReply(String error) {}
}
