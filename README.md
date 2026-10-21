# Spring AI en Action — Refund Management P0-I

POC d'accompagnement du talk Spring AI en Action : domaine Refund Management, capacités Spring AI et protections transversales. Aucune opération ne déclenche un paiement.

## Versions cible
- Java 21
- Spring Boot 4.0.8
- Spring AI 2.0.1
- Java 21+

## Build et lancement local
```bash
mvn clean verify
mvn -pl refund-api -am spring-boot:run -Dspring-boot.run.profiles=demo
```
Le build et les tests unitaires/MVC sont vérifiés. Les scénarios nécessitant Ollama, PostgreSQL/pgvector et le serveur MCP restent à lancer avec ces services disponibles.

## Identités de démonstration

Le profil `demo` active les comptes fictifs suivants, réservés à l'environnement local : `alice / alice-demo`, `bob / bob-demo` et `admin / admin-demo`. Ils utilisent HTTP Basic. Alice et Bob ont le rôle `USER`; admin possède aussi `ADMIN`.

## Exemples HTTP

```bash
curl -i -u alice:alice-demo localhost:8080/api/orders/CMD-1042
curl -i -u alice:alice-demo localhost:8080/api/orders/CMD-2099 # 403 attendu
curl -i -u alice:alice-demo localhost:8080/api/orders/CMD-1050/eligibility
curl -i -X POST -u alice:alice-demo localhost:8080/api/orders/CMD-1042/refund-drafts
curl -i -u admin:admin-demo localhost:8080/actuator/metrics
curl -i -u alice:alice-demo -X POST localhost:8080/api/ai/rag/admin/ingest # 403 attendu
```

## Sécurité
Les contrôleurs obtiennent désormais l'identité depuis Spring Security ; `X-Demo-Actor` et `X-Demo-User` ne sont plus utilisés. Les comptes codés en mémoire ne sont activés qu'avec `demo`. N'exposez pas ce profil sur un réseau public. L'authentification de démonstration ne remplace pas une configuration d'identité pour un produit réel.

`/api/**` exige une authentification, l'ingestion RAG et les endpoints Actuator hors santé exigent `ADMIN`, et `/actuator/health` est public. Les appels métier vérifient encore la propriété de la commande dans les services Java.

Les appels consultatifs à `/api/ai/safe-chat` sont bornés par un timeout applicatif et retournent un statut explicite `AI_UNAVAILABLE` en cas de panne technique connue. Un timeout Java ne garantit pas l'arrêt d'une requête déjà reçue par le fournisseur. Les exceptions métier et de sécurité ne sont pas converties en succès de repli.

Les métriques applicatives sont exposées via Actuator et Prometheus. Les observations Spring AI sont configurées sans contenu de prompt, de complétion ou de Tool. Le sampling configuré ne suffit pas à exporter des traces : aucun collecteur OpenTelemetry n'est branché dans cet incrément.

## Politique fictive de test
- `LAPTOP`: fenêtre de 30 jours, justificatif requis.
- `ACCESSORY`: fenêtre de 14 jours, justificatif non requis.
- Règles datées au 2026-01-01, version 1.0.
- `EligibilityService` applique une décision Java déterministe ; le futur LLM ne pourra ni déroger à cette règle ni déclencher une transaction.

## Limites du POC
- Les comptes et mots de passe de démonstration ne conviennent pas à un environnement partagé.
- La mémoire métier des commandes reste en fixtures ; le stockage vectoriel PostgreSQL est distinct.
- Le timeout applicatif complète les timeouts réseau, mais ne les remplace pas.
- Les scénarios nécessitant Ollama, pgvector et MCP doivent être vérifiés séparément avant leur démonstration.
