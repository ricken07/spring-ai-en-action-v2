package com.rickenbazolo.refund.api.lab;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record LabExecutionResult(
    String executionId,
    String scenarioId,
    String status,
    Object output,
    List<LabEvidence> evidence,
    Map<String, Object> diagnostics) {

  public LabExecutionResult {
    evidence = List.copyOf(evidence);
    diagnostics = Map.copyOf(diagnostics);
  }

  public record LabEvidence(
      Instant timestamp, String stage, String operation, String outcome, long durationMs) {}
}
