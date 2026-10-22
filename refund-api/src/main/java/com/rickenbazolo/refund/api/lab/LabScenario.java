package com.rickenbazolo.refund.api.lab;

public record LabScenario(
    String id,
    String title,
    String description,
    String defaultQuestion,
    boolean requiresConfirmation) {}
