package com.rickenbazolo.refund.ai.chat;

import com.rickenbazolo.refund.domain.RefundIntent;

public interface RefundChatGateway {

  String chat(String message);

  RefundIntent extractIntent(String message);
}
