# Slide → Code → Test → Preuve

Ce document relie le contenu projeté aux composants exécutés.

| Slides | Sujet | Code de référence | Vérification prévue | État de preuve |
|---|---|---|---|---|
| 1 | Spring AI en Action | — | Ouverture du talk | Sans objet |
| 2–3 | Application Spring Boot et responsabilités | `refund-application/.../OrderService`, `RefundService` | Tests du domaine/application | À rejouer avant conférence |
| 4 | Panorama Spring AI | `ai-chat/.../ChatClientConfig` | Build + contexte Spring | Build vérifié |
| 5 | Premier ChatClient | `ai-chat/.../SpringAiRefundChatGateway#chat` | `CHAT` | À exécuter avec Ollama |
| 6–8 | Structured Output et validation | `SpringAiRefundChatGateway#extractIntent`, `RefundIntentValidator` | `STRUCTURED` + cas incomplet | Tests unitaires; capture LLM à faire |
| 9–10 | Advisor et mémoire | `ChatMemoryConfig`, `RefundConversationService` | Deux appels `MEMORY` avec même conversation | À exécuter avec Ollama |
| 11–12 | Ingestion et RAG | `RefundPolicyIngestionService`, `RefundRagConfig` | Admin ingest puis `RAG` | À exécuter avec pgvector/Ollama |
| 13–14 | Retrieval, reranking, RAG modulaire | `AdvancedRagPipeline`, `RetrievalEvaluator` | `RAG_ADVANCED`, reranking ON/OFF | Tests déterministes; mesures locales à faire |
| 15–18 | Tool Calling et contrôle métier | `RefundReadTools`, `RefundBusinessAdapter`, `RefundDraftCommandService` | `TOOLS`, refus ACL, workflow sans confirmation | Tests unitaires; capture LLM à faire |
| 19–20 | MCP | `RefundMcpServerConfig`, `RefundMcpChatService` | `MCP`, découverte et appel distant | À exécuter avec serveur MCP démarré |
| 21–22 | Workflow déterministe | `RefundWorkflowService` | `WORKFLOW`, étapes applicatives retournées | Tests unitaires; capture LLM à faire |
| 23–24 | Agentic Loop et autonomie | `RefundInvestigationAgent`, `RefundOrchestrationFacade` | `AGENT`, Tools read-only réellement invoqués | Tests unitaires; capture LLM à faire |
| 25–26 | Sécurisation | `RefundSecurityConfig`, `CurrentUser` | 401/403, ACL sur commande | Tests MVC vérifiés |
| 27 | Observabilité et évaluation | `AiExecutionMetrics`, `RefundAnswerEvaluation` | Actuator, métriques, jeu de retrieval | Tests vérifiés; capture Actuator à faire |
| 28 | Dégradation contrôlée | `SafeRefundAiService`, `AiExecutionGuard` | `SAFE_CHAT`, panne et timeout simulés | Tests vérifiés; capture locale à faire |
| 29–30 | Architecture finale et synthèse | Modules P0-A à P0-I | Build global + runbook | Build vérifié; démo end-to-end à faire |
