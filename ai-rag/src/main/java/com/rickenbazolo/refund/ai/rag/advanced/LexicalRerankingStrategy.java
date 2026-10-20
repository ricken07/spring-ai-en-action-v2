package com.rickenbazolo.refund.ai.rag.advanced;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.stereotype.Component;

@Component
public class LexicalRerankingStrategy implements RerankingStrategy {

  @Override
  public List<RankedDocument> rerank(String query, List<RagCandidate> candidates, int limit) {
    if (limit <= 0) {
      throw new IllegalArgumentException("limit must be positive");
    }

    Set<String> queryTerms = tokenize(query);
    var sorted =
        candidates.stream()
            .map(
                candidate ->
                    new Scored(candidate, score(queryTerms, candidate.document().getText())))
            .sorted(
                Comparator.comparingDouble(Scored::score)
                    .reversed()
                    .thenComparingInt(scored -> scored.candidate().originalRank()))
            .limit(limit)
            .toList();

    return IntStream.range(0, sorted.size())
        .mapToObj(i -> new RankedDocument(sorted.get(i).candidate(), sorted.get(i).score(), i + 1))
        .toList();
  }

  private double score(Set<String> queryTerms, String content) {
    if (queryTerms.isEmpty()) {
      return 0.0;
    }
    Set<String> documentTerms = tokenize(content);
    long matching = queryTerms.stream().filter(documentTerms::contains).count();
    return (double) matching / queryTerms.size();
  }

  private Set<String> tokenize(String text) {
    if (text == null || text.isBlank()) {
      return Set.of();
    }
    String normalized =
        Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
    return Arrays.stream(normalized.split("[^a-z0-9]+"))
        .filter(token -> token.length() > 2)
        .collect(Collectors.toSet());
  }

  private record Scored(RagCandidate candidate, double score) {}
}
