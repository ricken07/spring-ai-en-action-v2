package com.rickenbazolo.refund.api.lab;

import com.rickenbazolo.refund.api.lab.LabExecutionResult.LabEvidence;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public final class LabEvidenceRecorder {

  private final List<LabEvidence> events = new ArrayList<>();

  public <T> T record(String stage, String operation, Supplier<T> action) {
    long start = System.nanoTime();
    String outcome = "SUCCESS";
    try {
      return action.get();
    } catch (RuntimeException exception) {
      outcome = "ERROR";
      throw exception;
    } finally {
      events.add(
          new LabEvidence(
              Instant.now(),
              stage,
              operation,
              outcome,
              TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)));
    }
  }

  public List<LabEvidence> snapshot() {
    return List.copyOf(events);
  }
}
