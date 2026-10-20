package com.rickenbazolo.refund.ai.rag;

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

@Service
public class RefundPolicyIngestionService {

  private final RefundPolicyCatalog catalog;
  private final VectorStore vectorStore;

  public RefundPolicyIngestionService(RefundPolicyCatalog catalog, VectorStore vectorStore) {
    this.catalog = catalog;
    this.vectorStore = vectorStore;
  }

  public int ingest() {
    List<Document> chunks = prepareDocuments();
    if (!chunks.isEmpty()) {
      vectorStore.add(chunks);
    }
    return chunks.size();
  }

  public List<Document> prepareDocuments() {
    List<Document> result = new ArrayList<>();
    var splitter =
        TokenTextSplitter.builder()
            .withChunkSize(300)
            .withMinChunkSizeChars(100)
            .withMinChunkLengthToEmbed(5)
            .withMaxNumChunks(100)
            .withKeepSeparator(true)
            .build();

    for (var source : catalog.sources()) {
      var reader = new TextReader(source.resource());
      reader.getCustomMetadata().put("policyId", source.policyId());
      reader.getCustomMetadata().put("version", source.version());
      reader.getCustomMetadata().put("category", "refund-policy");
      result.addAll(splitter.apply(reader.get()));
    }

    return List.copyOf(result);
  }
}
