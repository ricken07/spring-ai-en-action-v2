# Spring AI en Action — Refund Management P0-A

**Étape : fondations métier sans LLM, sans transaction financière.** Répertoire indépendant du dépôt historique.

## Versions cible
- Java 21
- Spring Boot 4.0.8 (POM fixé, build Maven non vérifié localement)
- Spring AI 2.0.1 (BOM réservé au parent ; **aucun module AI activé en P0-A**)

## Vérifier le coeur sans Maven
```bash
bash scripts/check-core.sh
```

## Vérifier l'application avec Maven 3.9+
```bash
mvn -U clean verify
mvn -pl refund-api -am spring-boot:run -Dspring-boot.run.profiles=demo
```
Le lancement Maven et les requêtes HTTP ne sont **pas vérifiés** dans l'environnement de génération (Maven absent). Le profil `demo` active explicitement les identités fictives pour un usage strictement local.

## API de démonstration
```bash
curl -i -H 'X-Demo-Actor: alice' localhost:8080/api/orders/CMD-1042
curl -i -H 'X-Demo-Actor: alice' localhost:8080/api/orders/CMD-2099 # 403 attendu
curl -i -H 'X-Demo-Actor: alice' localhost:8080/api/orders/CMD-1050/eligibility
curl -i -X POST -H 'X-Demo-Actor: alice' localhost:8080/api/orders/CMD-1042/refund-drafts
```
Ou `bash scenarios/requests.sh`.

## Sécurité
L'en-tête `X-Demo-Actor` est une **simulation d'identité**, pas une authentification. Il est désactivé par défaut. N'exposez **jamais** le profil demo en réseau public ni en production. P0 suivant : Spring Security, identité serveur établie et tests HTTP négatifs.

## Politique fictive de test
- `LAPTOP`: fenêtre de 30 jours, justificatif requis.
- `ACCESSORY`: fenêtre de 14 jours, justificatif non requis.
- Règles datées au 2026-01-01, version 1.0.
- `EligibilityService` applique une décision Java déterministe ; le futur LLM ne pourra ni déroger à cette règle ni déclencher une transaction.

## Limites P0-A
- Stockage mémoire (fixtures), PostgreSQL reporté à l'adaptateur suivant.
- Pas d'interface UI, pas de ChatClient, pas de Tool, pas de MCP.
- Tests du cœur exécutables au JDK seul, pas encore des tests JUnit Maven ni MockMvc.
- Avant toute mise en scène, compiler l'ensemble et vérifier les HTTP dans l'environnement cible.
