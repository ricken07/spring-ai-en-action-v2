package com.rickenbazolo.refund.ai.rag.advanced;

public record RankedDocument(RagCandidate candidate, double rerankingScore, int finalRank) {}
