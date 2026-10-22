package com.rickenbazolo.refund.api.lab;

import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class LabScenarioCatalog {

  private final List<LabScenario> scenarios =
      List.of(
          new LabScenario(
              "CHAT",
              "Premier appel au modèle",
              "Appel simple et réponse textuelle",
              "Je souhaite rembourser mon ordinateur.",
              false),
          new LabScenario(
              "STRUCTURED",
              "Sortie structurée",
              "Extraction de l'intention de remboursement",
              "Je veux rembourser CMD-1042 : mon ordinateur est défectueux.",
              false),
          new LabScenario(
              "MEMORY",
              "Conversation",
              "Mémoire et conseillers (Advisors)",
              "Ma commande est CMD-1042.",
              false),
          new LabScenario(
              "RAG",
              "RAG simple",
              "Recherche de politiques",
              "Quels justificatifs sont acceptés sans facture ?",
              false),
          new LabScenario(
              "RAG_ADVANCED",
              "RAG avancé",
              "Retrieval et reranking",
              "Quels justificatifs alternatifs sont autorisés ?",
              false),
          new LabScenario(
              "TOOLS",
              "Appel d'outils",
              "Accès aux services métier",
              "Consulte la commande CMD-1042.",
              false),
          new LabScenario(
              "MCP",
              "Serveur MCP",
              "Capacité documentaire distante",
              "Consulte REF-PROOF-001.",
              false),
          new LabScenario(
              "WORKFLOW",
              "Workflow déterministe",
              "Le code Java fixe les étapes",
              "Vérifie CMD-1042 et prépare une demande de remboursement.",
              true),
          new LabScenario(
              "AGENT",
              "Boucle agentique",
              "Le modèle choisit ses outils",
              "Analyse l'éligibilité de CMD-1042.",
              false),
          new LabScenario(
              "SAFE_CHAT",
              "Fiabilité",
              "Timeout et réponse de repli",
              "Explique la procédure de remboursement.",
              false));

  public List<LabScenario> all() {
    return scenarios;
  }

  public LabScenario require(String id) {
    return scenarios.stream()
        .filter(item -> item.id().equals(id))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Scénario de laboratoire inconnu: " + id));
  }
}
