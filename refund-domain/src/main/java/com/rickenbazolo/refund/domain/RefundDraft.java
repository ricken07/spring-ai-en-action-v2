package com.rickenbazolo.refund.domain;

public record RefundDraft(
    String draftId, String orderId, String status, EligibilityDecision eligibilitySnapshot) {}
