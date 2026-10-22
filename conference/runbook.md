# Runbook de conférence — Refund Management / Spring AI

## Préparation locale

Depuis la racine du dépôt :

```bash
java -version
mvn -version
docker --version
mvn clean verify
```

Démarrer PostgreSQL/pgvector :

```bash
docker compose -f infra/docker-compose.yml up -d
```

Vérifier les modèles Ollama configurés dans `refund-api/src/main/resources/application-demo.yml` et les récupérer au besoin avec `ollama pull <model>`. Démarrer le serveur MCP avant le backend :

```bash
mvn -pl refund-mcp-server -am install -DskipTests
mvn -pl refund-mcp-server spring-boot:run -Dspring-boot.run.profiles=demo
```

Dans un second terminal, démarrer Refund Management :

```bash
mvn -pl refund-api -am install -DskipTests
mvn -pl refund-api spring-boot:run -Dspring-boot.run.profiles=demo
```

Comptes de laboratoire, réservés à un usage local : `alice/alice-demo`, `bob/bob-demo`, `admin/admin-demo`.

## Vérifications rapides

```bash
curl -i http://localhost:8080/actuator/health
curl -u alice:alice-demo http://localhost:8080/api/lab/scenarios
curl -u admin:admin-demo http://localhost:8080/actuator/metrics
curl -u admin:admin-demo http://localhost:8080/api/ai/mcp/tools
```

Ouvrir ensuite `http://localhost:8080/` et s'authentifier avec le compte de démonstration. Le navigateur conserve HTTP Basic pour les requêtes same-origin. Ne pas exposer ce profil sur un réseau partagé.

Pour indexer les politiques, l'administrateur peut exécuter `POST /api/ai/rag/admin/ingest`. Ne pas réindexer à répétition avant l'idempotence de l'ingestion : la version actuelle peut créer des doublons.

## Parcours de laboratoire

```bash
curl -u alice:alice-demo \
  -X POST http://localhost:8080/api/lab/executions \
  -H 'Content-Type: application/json' \
  -d '{"scenarioId":"RAG_ADVANCED","question":"Quels justificatifs sont acceptés sans facture ?","conversationId":"conference","reranking":true,"confirmed":false}'
```

La réponse contient le résultat métier, un événement horodaté pour la frontière de scénario, et les diagnostics réellement émis par le service (par exemple les `policyId`, Tools suivis et étapes du workflow). Les détails internes non instrumentés ne sont pas reconstruits. Le mode `WORKFLOW` via l'exécuteur générique refuse `confirmed=true`; l'écriture n'est pas activable depuis cette interface.

Pour tester la mémoire, réutiliser le même `conversationId`. Elle est compartimentée par l'identité authentifiée. Pour l'isolation, rejouer avec Alice puis Bob et ne pas changer le compte du navigateur sans se déconnecter de son cache HTTP Basic.
