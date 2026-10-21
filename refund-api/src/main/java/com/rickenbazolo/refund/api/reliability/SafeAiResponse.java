package com.rickenbazolo.refund.api.reliability;

public record SafeAiResponse(String status, String message, boolean generatedByModel) {

  public static SafeAiResponse success(String message) {
    return new SafeAiResponse("SUCCESS", message, true);
  }

  public static SafeAiResponse unavailable() {
    return new SafeAiResponse(
        "AI_UNAVAILABLE",
        "L'assistant IA est temporairement indisponible. Les opérations métier restent accessibles"
            + " séparément.",
        false);
  }
}
