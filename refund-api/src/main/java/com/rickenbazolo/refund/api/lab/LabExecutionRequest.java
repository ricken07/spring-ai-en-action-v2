package com.rickenbazolo.refund.api.lab;

public record LabExecutionRequest(
    String scenarioId,
    String question,
    String conversationId,
    boolean reranking,
    boolean confirmed) {}
