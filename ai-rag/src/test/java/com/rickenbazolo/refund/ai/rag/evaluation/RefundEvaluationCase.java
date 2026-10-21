package com.rickenbazolo.refund.ai.rag.evaluation;

import java.util.Set;

public record RefundEvaluationCase(
    String id,
    String question,
    Set<String> expectedPolicyIds,
    Set<String> expectedTerms,
    boolean mustAbstain) {}
