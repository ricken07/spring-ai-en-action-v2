package com.rickenbazolo.refund.ai.rag.advanced;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RetrievalEvaluationDataset {

  public List<RetrievalCase> cases() {
    return List.of(
        new RetrievalCase(
            "Q01", "Quels justificatifs sont acceptés sans facture ?", Set.of("REF-PROOF-001")),
        new RetrievalCase(
            "Q02", "Quel est le délai ordinaire de remboursement ?", Set.of("REF-WINDOW-001")),
        new RetrievalCase(
            "Q03", "Comment traiter un ordinateur défectueux ?", Set.of("REF-DEFECT-001")),
        new RetrievalCase(
            "Q04", "Une confirmation de commande est-elle suffisante ?", Set.of("REF-PROOF-001")),
        new RetrievalCase(
            "Q05",
            "Quelles vérifications prévoir pour un produit défectueux ?",
            Set.of("REF-DEFECT-001")));
  }
}
