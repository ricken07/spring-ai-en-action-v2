package com.rickenbazolo.refund.api.lab;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Bounded in-memory execution metadata, scoped to the authenticated owner. */
@Component
public class LabEvidenceStore {

  private static final int MAX_EXECUTIONS = 50;
  private final Map<String, StoredExecution> executions = new LinkedHashMap<>();

  public synchronized void save(String ownerId, LabExecutionResult result) {
    if (ownerId == null || ownerId.isBlank()) {
      throw new SecurityException("Authenticated user required");
    }
    executions.put(result.executionId(), new StoredExecution(ownerId, result));
    while (executions.size() > MAX_EXECUTIONS) {
      executions.remove(executions.keySet().iterator().next());
    }
  }

  public synchronized Optional<LabExecutionResult> find(String ownerId, String executionId) {
    StoredExecution stored = executions.get(executionId);
    if (stored == null || !stored.ownerId().equals(ownerId)) {
      return Optional.empty();
    }
    return Optional.of(stored.result());
  }

  private record StoredExecution(String ownerId, LabExecutionResult result) {}
}
