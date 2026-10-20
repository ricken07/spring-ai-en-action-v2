package com.rickenbazolo.refund.ai.rag.advanced;

public interface RagAnswerGenerator {

  String generate(String question, String context);
}
