package com.rickenbazolo.refund.ai.rag.advanced;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RetrievalEvaluator {

  public double hitAtK(Set<String> expectedIds, List<String> rankedIds, int k) {
    validateK(k);
    return rankedIds.stream().limit(k).anyMatch(expectedIds::contains) ? 1.0 : 0.0;
  }

  public double reciprocalRank(Set<String> expectedIds, List<String> rankedIds, int k) {
    validateK(k);
    for (int i = 0; i < Math.min(k, rankedIds.size()); i++) {
      if (expectedIds.contains(rankedIds.get(i))) {
        return 1.0 / (i + 1);
      }
    }
    return 0.0;
  }

  public double meanReciprocalRank(
      List<RetrievalCase> cases, List<List<String>> rankedResults, int k) {
    validateK(k);
    if (cases.isEmpty() || cases.size() != rankedResults.size()) {
      throw new IllegalArgumentException("Invalid evaluation dataset");
    }
    double total = 0.0;
    for (int i = 0; i < cases.size(); i++) {
      total += reciprocalRank(cases.get(i).expectedPolicyIds(), rankedResults.get(i), k);
    }
    return total / cases.size();
  }

  private void validateK(int k) {
    if (k <= 0) {
      throw new IllegalArgumentException("k must be positive");
    }
  }
}
