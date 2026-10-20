package com.rickenbazolo.refund.ai.rag.advanced;

import java.util.List;

public interface RerankingStrategy {

  List<RankedDocument> rerank(String query, List<RagCandidate> candidates, int limit);
}
