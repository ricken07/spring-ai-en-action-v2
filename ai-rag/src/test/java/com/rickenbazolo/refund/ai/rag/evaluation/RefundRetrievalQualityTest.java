package com.rickenbazolo.refund.ai.rag.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rickenbazolo.refund.ai.rag.advanced.RetrievalEvaluator;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RefundRetrievalQualityTest {

  private final RetrievalEvaluator retrievalEvaluator = new RetrievalEvaluator();
  private final RefundAnswerEvaluation answerEvaluator = new RefundAnswerEvaluation();

  @Test
  void shouldFindProofPolicyInTopThreeAndCheckAnswerEvidence() {
    var rankedPolicyIds = List.of("REF-WINDOW-001", "REF-PROOF-001", "REF-DEFECT-001");
    var evaluationCase =
        new RefundEvaluationCase(
            "Q-PROOF-01",
            "Quels justificatifs utiliser si la facture est perdue ?",
            Set.of("REF-PROOF-001"),
            Set.of("confirmation de commande", "après vérification"),
            false);

    assertEquals(
        1.0, retrievalEvaluator.hitAtK(evaluationCase.expectedPolicyIds(), rankedPolicyIds, 3));
    assertTrue(
        answerEvaluator.hasExpectedSources(rankedPolicyIds, evaluationCase.expectedPolicyIds()));
    assertTrue(
        answerEvaluator.containsExpectedTerms(
            "Une confirmation de commande peut être utilisée après vérification.",
            evaluationCase.expectedTerms()));
    assertTrue(
        answerEvaluator.matchesExpectedAbstention(
            RefundAnswerEvaluation.ResponseOutcome.ANSWERED, false));
  }

  @Test
  void shouldEvaluateAbstentionAsStructuredOutcome() {
    var evaluationCase =
        new RefundEvaluationCase(
            "Q-OUT-01", "Quelle politique régit les voyages spatiaux ?", Set.of(), Set.of(), true);

    assertTrue(
        answerEvaluator.matchesExpectedAbstention(
            RefundAnswerEvaluation.ResponseOutcome.INSUFFICIENT_EVIDENCE,
            evaluationCase.mustAbstain()));
    assertFalse(
        answerEvaluator.matchesExpectedAbstention(
            RefundAnswerEvaluation.ResponseOutcome.ANSWERED, evaluationCase.mustAbstain()));
    assertFalse(
        answerEvaluator.matchesExpectedAbstention(
            RefundAnswerEvaluation.ResponseOutcome.UNAVAILABLE, evaluationCase.mustAbstain()));
  }
}
