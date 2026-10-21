package com.rickenbazolo.refund.ai.tools;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Component;

/** Keeps per-request tool names only; order identifiers and results are never recorded. */
@Component
public class ToolInvocationTracker {

  private final ConcurrentHashMap<String, CopyOnWriteArrayList<String>> calls =
      new ConcurrentHashMap<>();

  public void record(String requestId, String toolName) {
    calls.computeIfAbsent(requestId, ignored -> new CopyOnWriteArrayList<>()).add(toolName);
  }

  public List<String> drain(String requestId) {
    var recorded = calls.remove(requestId);
    return recorded == null ? List.of() : List.copyOf(recorded);
  }
}
