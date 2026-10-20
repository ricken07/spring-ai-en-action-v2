package com.rickenbazolo.refund.ai.chat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class ConversationScope {

  private ConversationScope() {}

  public static String key(String userId, String conversationId) {
    requireValid(userId, "userId");
    requireValid(conversationId, "conversationId");

    return "refund:" + encode(userId) + ":" + encode(conversationId);
  }

  private static String encode(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static void requireValid(String value, String field) {
    if (value == null || !value.matches("[A-Za-z0-9_-]{1,64}")) {
      throw new IllegalArgumentException("Invalid " + field);
    }
  }
}
