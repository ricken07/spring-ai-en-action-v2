package com.rickenbazolo.refund.ai.rag.evaluation;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RefundAnswerEvaluation {

  public boolean containsExpectedTerms(String answer, Set<String> expectedTerms) {
    String normalizedAnswer = normalize(answer);
    return expectedTerms.stream().map(this::normalize).allMatch(normalizedAnswer::contains);
  }

  public boolean hasExpectedSources(
      List<String> retrievedPolicyIds, Set<String> expectedPolicyIds) {
    return retrievedPolicyIds.stream().anyMatch(expectedPolicyIds::contains);
  }

  public boolean matchesExpectedAbstention(ResponseOutcome outcome, boolean mustAbstain) {
    return mustAbstain
        ? outcome == ResponseOutcome.INSUFFICIENT_EVIDENCE
        : outcome == ResponseOutcome.ANSWERED;
  }

  private String normalize(String text) {
    return Normalizer.normalize(text == null ? "" : text, Normalizer.Form.NFD)
        .replaceAll("\\p{M}+", "")
        .toLowerCase(Locale.ROOT);
  }

  public enum ResponseOutcome {
    ANSWERED,
    INSUFFICIENT_EVIDENCE,
    UNAVAILABLE
  }
}
