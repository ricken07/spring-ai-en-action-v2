package com.rickenbazolo.refund.mcpserver;

import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class RefundPolicyService {

  private final Map<String, RefundPolicy> policies =
      Map.of(
          "REF-PROOF-001",
          new RefundPolicy(
              "REF-PROOF-001",
              "Justificatifs d'achat",
              "1.0",
              """
              La facture est le justificatif standard.
              Si elle n'est plus disponible, une confirmation de commande associée au compte client
              peut être utilisée comme justificatif alternatif, après vérification.
              La réponse à une question de politique n'accorde pas un remboursement.
              """),
          "REF-DEFECT-001",
          new RefundPolicy(
              "REF-DEFECT-001",
              "Produit défectueux",
              "1.0",
              """
              Une demande concernant un produit défectueux doit inclure la référence de commande et
              une description du défaut signalé.
              L'éligibilité doit être déterminée par les services métier de l'application.
              """));

  public Optional<RefundPolicy> findById(String policyId) {
    if (policyId == null || policyId.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(policies.get(policyId));
  }
}
