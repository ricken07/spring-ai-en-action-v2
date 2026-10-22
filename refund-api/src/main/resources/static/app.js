const LABELS = {
  status: { SUCCESS: "Succès", ERROR: "Erreur", NEEDS_CLARIFICATION: "Précisions nécessaires", CONFIRMATION_REQUIRED: "Confirmation requise", COMPLETED: "Terminé", UNSUPPORTED_INTENT: "Intention non prise en charge", NOT_ELIGIBLE: "Non éligible", DRAFT_CREATED: "Brouillon créé", AI_UNAVAILABLE: "IA indisponible" },
  stage: { APPLICATION: "Application" },
  operation: { RUN_CHAT: "Exécution du chat", RUN_STRUCTURED: "Sortie structurée", RUN_MEMORY: "Exécution avec mémoire", RUN_RAG: "Exécution RAG", RUN_RAG_ADVANCED: "Exécution RAG avancé", RUN_TOOLS: "Exécution avec outils", RUN_MCP: "Exécution MCP", RUN_WORKFLOW: "Exécution du workflow", RUN_AGENT: "Exécution de l’agent", RUN_SAFE_CHAT: "Exécution du chat sécurisé" },
  step: {
    EXTRACT_INTENT: "Extraction de l’intention par le modèle",
    VALIDATION_FAILED: "Validation de l’intention échouée",
    NON_REFUND_INTENT: "Intention autre qu’un remboursement",
    VALIDATE_INTENT: "Validation de l’intention",
    GET_AUTHORIZED_ORDER: "Lecture de la commande autorisée",
    CHECK_ELIGIBILITY: "Contrôle d’éligibilité",
    AWAIT_CONFIRMATION: "Attente de confirmation",
    CREATE_DRAFT: "Création du brouillon",
  },
};
const label = (group, value) => LABELS[group][value] ?? value;

const form = document.getElementById("execution-form");
const scenarioSelect = document.getElementById("scenario");
const questionInput = document.getElementById("question");
const rerankingInput = document.getElementById("reranking");
const button = document.getElementById("execute");
const resultContent = document.getElementById("result-content");
const technicalContent = document.getElementById("technical-content");
const resultStatus = document.getElementById("result-status");
const resultSubtitle = document.getElementById("result-subtitle");
const evidenceCount = document.getElementById("evidence-count");

let scenarios = [];
let conversationId = loadConversationId();

function loadConversationId() {
  try {
    const stored = sessionStorage.getItem("conversationId");
    if (stored) return stored;
  } catch {
    // Stockage indisponible : l'identifiant reste valable pour la page courante.
  }
  return newConversationId();
}

// La mémoire de conversation est vidée côté serveur, puis un nouvel identifiant est utilisé.
async function resetConversation() {
  const previous = conversationId;
  conversationId = newConversationId();
  try {
    await fetch(`/api/lab/conversations/${encodeURIComponent(previous)}`, { method: "DELETE", credentials: "same-origin" });
  } catch {
    // L'ancien identifiant n'est plus utilisé : l'échec du nettoyage n'a pas d'effet visible.
  }
}

function newConversationId() {
  const id = crypto.randomUUID();
  try {
    sessionStorage.setItem("conversationId", id);
  } catch {
    // Stockage indisponible : l'identifiant reste valable pour la page courante.
  }
  return id;
}

function element(tag, className, text) {
  const node = document.createElement(tag);
  if (className) node.className = className;
  if (text !== undefined) node.textContent = text;
  return node;
}

function selectedScenario() {
  return scenarios.find((item) => item.id === scenarioSelect.value);
}

function updateScenarioFields() {
  const scenario = selectedScenario();
  if (!scenario) return;

  questionInput.value = scenario.defaultQuestion;
  document.getElementById("scenario-description").textContent = scenario.description;
  document.getElementById("conversation-field").hidden = scenario.id !== "MEMORY";
  document.getElementById("new-conversation").hidden = scenario.id !== "MEMORY";
  document.getElementById("reranking-field").hidden = scenario.id !== "RAG_ADVANCED";
}

function addTextRow(parent, label, value, className = "") {
  const row = element("div", `detail-row ${className}`.trim());
  row.append(element("dt", "detail-label", label));
  row.append(element("dd", "detail-value", value ?? "—"));
  parent.append(row);
}

const INLINE_MARKDOWN = /(`[^`]+`)|(\*\*[^*]+\*\*|__[^_]+__)|(\*[^*\s][^*]*\*|_[^_\s][^_]*_)|(\[[^\]]+\]\(https?:\/\/[^)\s]+\))/g;

// Le Markdown du LLM est converti en nœuds DOM : aucun HTML du modèle n'est interprété.
function appendInlineMarkdown(parent, text) {
  let last = 0;
  for (const match of text.matchAll(INLINE_MARKDOWN)) {
    parent.append(text.slice(last, match.index));
    const [token, code, strong, emphasis, link] = match;
    if (code) {
      parent.append(element("code", "", token.slice(1, -1)));
    } else if (strong) {
      const node = element("strong");
      appendInlineMarkdown(node, token.slice(2, -2));
      parent.append(node);
    } else if (emphasis) {
      const node = element("em");
      appendInlineMarkdown(node, token.slice(1, -1));
      parent.append(node);
    } else if (link) {
      const split = token.indexOf("](");
      const anchor = element("a", "", token.slice(1, split));
      anchor.href = token.slice(split + 2, -1);
      anchor.target = "_blank";
      anchor.rel = "noopener noreferrer";
      parent.append(anchor);
    }
    last = match.index + token.length;
  }
  parent.append(text.slice(last));
}

function renderMarkdown(markdown) {
  const root = element("div", "answer-copy markdown");
  const lines = markdown.replace(/\r\n?/g, "\n").split("\n");
  let paragraph = [];
  let list = null;

  const flushParagraph = () => {
    if (!paragraph.length) return;
    const node = element("p");
    appendInlineMarkdown(node, paragraph.join(" "));
    root.append(node);
    paragraph = [];
  };
  const flushList = () => {
    list = null;
  };

  for (let index = 0; index < lines.length; index++) {
    const line = lines[index];
    const fence = line.match(/^\s*```/);
    const heading = line.match(/^(#{1,6})\s+(.*)$/);
    const item = line.match(/^\s*([-*+]|\d+[.)])\s+(.*)$/);
    const quote = line.match(/^>\s?(.*)$/);

    if (fence) {
      flushParagraph();
      flushList();
      const code = [];
      while (++index < lines.length && !/^\s*```/.test(lines[index])) code.push(lines[index]);
      root.append(element("pre", "", code.join("\n")));
    } else if (!line.trim()) {
      flushParagraph();
      flushList();
    } else if (heading) {
      flushParagraph();
      flushList();
      const node = element(`h${Math.min(heading[1].length + 2, 6)}`);
      appendInlineMarkdown(node, heading[2]);
      root.append(node);
    } else if (item) {
      flushParagraph();
      const ordered = /\d/.test(item[1]);
      if (!list || list.ordered !== ordered) {
        list = { ordered, node: element(ordered ? "ol" : "ul") };
        root.append(list.node);
      }
      const node = element("li");
      appendInlineMarkdown(node, item[2]);
      list.node.append(node);
    } else if (quote) {
      flushParagraph();
      flushList();
      const node = element("blockquote");
      appendInlineMarkdown(node, quote[1]);
      root.append(node);
    } else {
      flushList();
      paragraph.push(line.trim());
    }
  }
  flushParagraph();
  return root;
}

function renderTextAnswer(answer) {
  if (!answer) return element("div", "answer-copy", "Aucune réponse textuelle n’a été fournie.");
  return renderMarkdown(answer);
}

function renderList(title, items, className = "") {
  const section = element("section", `output-section ${className}`.trim());
  section.append(element("h3", "output-heading", title));
  if (!Array.isArray(items) || items.length === 0) {
    section.append(element("p", "muted-copy", "Aucun élément retourné."));
    return section;
  }
  const list = element("ul", "evidence-list");
  for (const item of items) list.append(element("li", "evidence-item", String(item)));
  section.append(list);
  return section;
}

function renderPolicyReferences(documents, advanced = false) {
  const section = element("section", "output-section");
  section.append(element("h3", "output-heading", "Politiques retrouvées"));
  if (!Array.isArray(documents) || documents.length === 0) {
    section.append(element("p", "muted-copy", "Aucun document retourné."));
    return section;
  }

  const list = element("div", "policy-list");
  for (const doc of documents) {
    const item = element("article", "policy-item");
    const top = element("div", "policy-topline");
    top.append(element("strong", "policy-id", doc.policyId || "Politique sans identifiant"));
    if (advanced) top.append(element("span", "rank-chip", `rang ${doc.originalRank} → ${doc.finalRank}`));
    else if (doc.source) top.append(element("span", "source-name", doc.source));
    item.append(top);
    if (advanced && Number.isFinite(doc.retrievalScore)) {
      const scores = element("p", "score-line", `Recherche ${doc.retrievalScore.toFixed(3)} · reclassement ${Number(doc.rerankingScore).toFixed(3)}`);
      item.append(scores);
    }
    if (doc.excerpt) item.append(element("p", "policy-excerpt", doc.excerpt));
    list.append(item);
  }
  section.append(list);
  return section;
}

const MESSAGE_ROLES = { USER: "Utilisateur", ASSISTANT: "Assistant", SYSTEM: "Système", TOOL: "Outil" };

function renderConversationTree(userId, messages) {
  const section = element("section", "output-section");
  section.append(element("h3", "output-heading", "Historique de la conversation"));
  const root = element("ul", "conversation-tree");
  const user = element("li", "tree-user");
  user.append(element("span", "tree-user-name", `Utilisateur : ${userId ?? "—"}`));
  const branch = element("ul", "conversation-tree");
  for (const message of messages ?? []) {
    const item = element("li", `tree-message tree-${message.type.toLowerCase()}`);
    item.append(element("strong", "tree-role", MESSAGE_ROLES[message.type] ?? message.type));
    const text = element("div", "tree-text");
    if (message.type === "ASSISTANT") text.append(renderMarkdown(message.content ?? ""));
    else text.textContent = message.content ?? "";
    item.append(text);
    branch.append(item);
  }
  user.append(branch);
  root.append(user);
  section.append(root);
  return section;
}

function renderModeTag(text, className) {
  return element("p", `mode-tag ${className}`, text);
}

function renderSteps(title, steps, note) {
  const section = element("section", "output-section");
  section.append(element("h3", "output-heading", title));
  const list = element("ol", "step-list");
  for (const step of steps ?? []) list.append(element("li", "step-item", label("step", step)));
  section.append(list);
  if (note) section.append(element("p", "muted-copy", note));
  return section;
}

// Parcours ordonné des outils appelés par le modèle, avec le nombre d'appels par outil.
function renderToolJourney(title, invocations) {
  const section = element("section", "output-section compact-section");
  section.append(element("h3", "output-heading", title));
  if (!invocations.length) {
    section.append(element("p", "muted-copy", "Le modèle n’a appelé aucun outil."));
    return section;
  }
  const counts = new Map();
  for (const name of invocations) counts.set(name, (counts.get(name) ?? 0) + 1);
  const total = invocations.length;
  section.append(element("p", "journey-summary", `${total} appel${total > 1 ? "s" : ""} · ${counts.size} outil${counts.size > 1 ? "s" : ""} distinct${counts.size > 1 ? "s" : ""}`));
  const list = element("ol", "step-list");
  for (const name of invocations) list.append(element("li", "step-item", name));
  section.append(list);
  const tally = element("p", "journey-tally", [...counts].map(([name, count]) => `${name} ×${count}`).join(" · "));
  section.append(tally);
  return section;
}

function renderOutput(result) {
  const output = result.output;
  const scenarioId = result.scenarioId;
  const content = element("div", "rendered-output");
  if (output == null) {
    content.append(element("p", "muted-copy", "Aucun résultat métier n’a été retourné."));
    return content;
  }

  if (scenarioId === "CHAT") return renderTextAnswer(output);

  if (scenarioId === "STRUCTURED") {
    content.append(element("span", output.valid ? "state-badge state-success" : "state-badge state-warning", output.valid ? "Intention valide" : "À clarifier"));
    if (output.intent) {
      const details = element("dl", "detail-list");
      addTextRow(details, "Commande", output.intent.orderId);
      addTextRow(details, "Motif", output.intent.reason);
      addTextRow(details, "Action", output.intent.requestedAction);
      content.append(details);
    }
    if (output.errors?.length) content.append(renderList("Champs à compléter", output.errors));
    return content;
  }

  if (scenarioId === "MEMORY") {
    content.append(renderTextAnswer(output.answer));
    if (output.snapshot) {
      const details = element("dl", "detail-list inline-details");
      addTextRow(details, "Messages mémorisés", output.snapshot.storedMessages);
      content.append(details);
      content.append(renderConversationTree(output.userId, output.snapshot.messages));
    }
    return content;
  }

  if (scenarioId === "RAG") {
    content.append(renderTextAnswer(output.answer));
    content.append(renderPolicyReferences(output.retrievedPolicies));
    return content;
  }

  if (scenarioId === "RAG_ADVANCED") {
    content.append(renderTextAnswer(output.answer));
    content.append(renderPolicyReferences(output.documents, true));
    if (output.timing) {
      const timing = element("div", "timing-strip");
      for (const [label, value] of [["Recherche", output.timing.retrievalMs], ["Reclassement", output.timing.rerankingMs], ["Génération", output.timing.generationMs], ["Total", output.timing.totalMs]]) {
        const cell = element("div", "timing-cell");
        cell.append(element("span", "timing-label", label), element("strong", "timing-value", `${value} ms`));
        timing.append(cell);
      }
      content.append(timing);
    }
    return content;
  }

  if (scenarioId === "TOOLS" || scenarioId === "MCP") {
    content.append(renderTextAnswer(output.answer));
    content.append(renderList("Outils réellement invoqués", output.invokedTools));
    return content;
  }

  if (scenarioId === "WORKFLOW") {
    content.append(renderModeTag("Chemin fixé par le code Java", "mode-deterministic"));
    content.append(element("span", output.status === "CONFIRMATION_REQUIRED" ? "state-badge state-warning" : "state-badge state-info", label("status", output.status) || "Workflow terminé"));
    content.append(renderSteps("Étapes décidées par le code Java", output.executedSteps, "Pour une même entrée, la séquence est toujours la même : le modèle n’intervient que pour extraire l’intention."));
    if (output.eligibility) {
      const details = element("dl", "detail-list inline-details");
      addTextRow(details, "Éligibilité", output.eligibility.reason);
      content.append(details);
    }
    return content;
  }

  if (scenarioId === "AGENT") {
    content.append(renderModeTag("Chemin choisi par le modèle", "mode-agentic"));
    content.append(renderTextAnswer(output.answer));
    return content;
  }

  if (scenarioId === "SAFE_CHAT") {
    content.append(element("span", output.generatedByModel ? "state-badge state-success" : "state-badge state-warning", output.generatedByModel ? "Réponse du modèle" : "Réponse de repli · non générée"));
    content.append(renderTextAnswer(output.message));
    return content;
  }

  content.append(element("pre", "json-output", JSON.stringify(output, null, 2)));
  return content;
}

function renderDiagnostics(result) {
  technicalContent.replaceChildren();
  const evidence = result.evidence || [];
  evidenceCount.textContent = evidence.length === 1 ? "1 événement observé" : `${evidence.length} événements observés`;

  const summary = element("div", "execution-summary");
  summary.append(element("span", "execution-id", `Exécution ${result.executionId}`));
  summary.append(element("span", "execution-result", `Statut d’exécution : ${label("status", result.status)}`));
  technicalContent.append(summary);

  if (evidence.length) {
    const timeline = element("ol", "timeline");
    for (const event of evidence) {
      const item = element("li", "timeline-event");
      const top = element("div", "timeline-top");
      top.append(element("strong", "timeline-operation", label("operation", event.operation)));
      top.append(element("span", event.outcome === "SUCCESS" ? "event-state event-success" : "event-state event-error", label("status", event.outcome)));
      item.append(top);
      item.append(element("p", "timeline-meta", `${label("stage", event.stage)} · ${event.durationMs} ms · ${new Date(event.timestamp).toLocaleTimeString("fr-FR")}`));
      timeline.append(item);
    }
    technicalContent.append(timeline);
  } else {
    technicalContent.append(element("p", "technical-empty", "Aucun événement technique n’a été instrumenté pour cette exécution."));
  }

  const diagnostics = result.diagnostics || {};
  technicalContent.append(element("p", "scope-note", diagnostics.evidenceScope || "Les diagnostics détaillés dépendent de l’instrumentation du scénario."));
  if (diagnostics.toolInvocations) {
    const title = result.scenarioId === "AGENT" ? "Parcours de la boucle agentique" : "Outils appelés, dans l’ordre";
    technicalContent.append(renderToolJourney(title, diagnostics.toolInvocations));
  }
  if (diagnostics.workflowSteps) technicalContent.append(renderSteps("Étapes déterministes observées", diagnostics.workflowSteps));
  if (diagnostics.retrievedPolicyIds) technicalContent.append(renderList("Identifiants de politiques retrouvés", diagnostics.retrievedPolicyIds, "compact-section"));
  if (diagnostics.responseStatus) addTextRow(technicalContent, "Réponse applicative", `${label("status", diagnostics.responseStatus)} · générée par le modèle : ${diagnostics.generatedByModel ? "oui" : "non"}`);
  if (diagnostics.businessStatus) addTextRow(technicalContent, "Statut métier", label("status", diagnostics.businessStatus));
  if (diagnostics.timing) addTextRow(technicalContent, "Latences mesurées", `${diagnostics.timing.retrievalMs} / ${diagnostics.timing.rerankingMs} / ${diagnostics.timing.generationMs} / ${diagnostics.timing.totalMs} ms (recherche / reclassement / génération / total)`);
  if (diagnostics.errorType) {
    const error = element("div", "notice notice-error");
    error.append(element("strong", "", "L’exécution a échoué."));
    error.append(element("span", "", `Type technique : ${diagnostics.errorType}. Les détails internes sont masqués.`));
    technicalContent.append(error);
  }
}

async function loadScenarios() {
  const response = await fetch("/api/lab/scenarios", { credentials: "same-origin" });
  if (!response.ok) throw new Error(response.status === 401 ? "Authentification requise" : "Impossible de charger le catalogue");
  scenarios = await response.json();
  scenarioSelect.replaceChildren();
  for (const item of scenarios) {
    const option = document.createElement("option");
    option.value = item.id;
    option.textContent = item.title;
    scenarioSelect.append(option);
  }
  scenarioSelect.disabled = false;
  button.disabled = false;
  scenarioSelect.value = "RAG";
  updateScenarioFields();
}

async function executeScenario(event) {
  event.preventDefault();
  const scenario = selectedScenario();
  if (!scenario || !questionInput.value.trim()) return;

  button.disabled = true;
  button.classList.add("is-loading");
  button.querySelector(".button-label").textContent = "Exécution en cours";
  resultContent.setAttribute("aria-busy", "true");
  resultContent.replaceChildren(element("div", "loading-state", "L’application exécute le scénario et collecte les diagnostics disponibles…"));
  resultStatus.className = "state-badge state-running";
  resultStatus.textContent = "En cours";
  resultSubtitle.textContent = scenario.title;
  technicalContent.replaceChildren(element("p", "technical-empty", "Collecte des événements d’exécution…"));

  const request = {
    scenarioId: scenario.id,
    question: questionInput.value.trim(),
    conversationId: scenario.id === "MEMORY" ? conversationId : null,
    reranking: scenario.id === "RAG_ADVANCED" && rerankingInput.checked,
    confirmed: false
  };

  try {
    const response = await fetch("/api/lab/executions", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(request)
    });
    const result = await response.json();
    if (!response.ok) throw new Error(result.error || `HTTP ${response.status}`);

    resultContent.replaceChildren(renderOutput(result));
    renderDiagnostics(result);
    resultStatus.className = result.status === "SUCCESS" ? "state-badge state-success" : "state-badge state-error";
    resultStatus.textContent = result.status === "SUCCESS" ? "Terminé" : "Échec";
    resultSubtitle.textContent = `${scenario.title} · ${result.executionId}`;
  } catch (error) {
    resultStatus.className = "state-badge state-error";
    resultStatus.textContent = "Erreur";
    resultSubtitle.textContent = "La requête n’a pas abouti.";
    resultContent.replaceChildren(element("div", "notice notice-error", error.message || "Le backend ne répond pas. Vérifiez le service puis réessayez."));
    technicalContent.replaceChildren(element("p", "technical-empty", "Aucune preuve d’exécution n’a été reçue."));
    evidenceCount.textContent = "Aucune preuve";
  } finally {
    button.disabled = false;
    button.classList.remove("is-loading");
    button.querySelector(".button-label").textContent = "Exécuter le scénario";
    resultContent.setAttribute("aria-busy", "false");
  }
}

scenarioSelect.addEventListener("change", () => {
  updateScenarioFields();
  resetConversation();
});
document.getElementById("new-conversation").addEventListener("click", resetConversation);
form.addEventListener("submit", executeScenario);
loadScenarios().catch((error) => {
  scenarioSelect.replaceChildren(new Option("Catalogue indisponible", ""));
  resultStatus.className = "state-badge state-error";
  resultStatus.textContent = "Connexion requise";
  resultSubtitle.textContent = error.message;
  resultContent.replaceChildren(element("div", "notice notice-error", `${error.message}. Rechargez la page après authentification.`));
});
