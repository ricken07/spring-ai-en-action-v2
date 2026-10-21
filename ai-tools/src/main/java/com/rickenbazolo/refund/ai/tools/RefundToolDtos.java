package com.rickenbazolo.refund.ai.tools;

public final class RefundToolDtos {

  private RefundToolDtos() {}

  public record OrderView(String orderId, String status, String product, String purchaseDate) {}

  public record EligibilityView(String orderId, boolean eligible, String reason) {}

  public record DraftView(String draftId, String orderId, String status) {}
}
