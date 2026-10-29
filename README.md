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

## Schémas principaux

Schémas Mermaid extraits de la présentation, dans l'ordre du talk. Sur GitHub, survoler un schéma affiche les contrôles pour zoomer, déplacer et agrandir.

### Une application Spring Boot. Un nouveau besoin.

L'application fonctionne déjà ; l'IA doit **s'intégrer à ses points d'extension**, pas remplacer ses services.

```mermaid
flowchart LR
  U[Utilisateur] --> API[REST API Spring Boot]
  API --> O[OrderService]
  API --> E[EligibilityService]
  API --> D[RefundService]
  O --> DB[(Commandes)]
  E --> DB
  D --> DB
```

### Un appel LLM ne fait pas une intégration

Une réponse générée ne donne pas automatiquement accès aux données, aux règles ou aux autorisations de l'application.

```mermaid
flowchart LR
  U[Demande libre] --> M[LLM]
  M --> R[Réponse textuelle]
  X[Services métier et politiques] -. pas automatiquement accessibles .- M
```

### Spring AI : intégrer les capacités des modèles dans Spring

Spring AI fournit des abstractions pour **composer les capacités des modèles** dans une application Spring Boot.

```mermaid
flowchart TB
  APP[Application Spring Boot] --> AI[Spring AI]
  AI --> MODEL[Modèles]
  AI --> CONTEXT[Advisors / mémoire]
  AI --> RAG[RAG / VectorStore]
  AI --> TOOLS[Tools / MCP]
  APP --> BUSINESS[Services métier Java]
```

### Structured Output : validation et limites

La conversion en objet Java ne remplace pas la validation applicative.

```mermaid
flowchart LR
  M[Réponse du modèle] --> I[RefundIntent]
  I --> V{Validation Java}
  V -->|Complet| O[Intention exploitable]
  V -->|Incomplet| C[Demander une précision]
```

### Advisors : enrichir les interactions avec le modèle

Un Advisor est un composant Spring AI qui intercepte une interaction ChatClient pour enrichir la requête et/ou traiter la réponse.

```mermaid
flowchart LR
    U[Demande utilisateur] --> C[ChatClient]
    C --> A[Advisor]
    A --> M[ChatModel / LLM]
    M --> A
    A --> R[Réponse à l'application]
```

### Chat Memory : conserver le contexte conversationnel

ChatMemory conserve l'historique des échanges ; `MessageChatMemoryAdvisor` le réinjecte dans les appels au modèle, selon l'identifiant de conversation.

```mermaid
sequenceDiagram
    participant U as Utilisateur
    participant A as ChatClient + Memory Advisor
    participant H as ChatMemory
    participant L as LLM
    U->>A: « Ma commande est CMD-1042 »
    A->>H: get(conversationKey)
    A->>L: Message + contexte disponible
    L-->>A: Réponse
    A->>H: add(conversationKey, messages)
    A-->>U: Réponse
    U->>A: « Et son remboursement ? »
    A->>H: get(conversationKey)
    A->>L: Question + historique CMD-1042
    L-->>A: Réponse contextualisée
    A-->>U: Réponse
```

### RAG : ingestion et indexation vectorielle

Préparer les documents métier pour permettre leur recherche par similarité sémantique.

```mermaid
flowchart LR
    A[Politiques de remboursement] --> B[DocumentReader]
    B --> C[TokenTextSplitter]
    C --> D[Embedding Model]
    D --> E[(pgvector)]
```

### RAG : retrieval avec QuestionAnswerAdvisor

`QuestionAnswerAdvisor` retrouve les passages pertinents dans le `VectorStore` et enrichit le prompt transmis au modèle.

```mermaid
flowchart LR
    Q[Question] --> A[QuestionAnswerAdvisor]
    A --> V[(VectorStore)]
    V --> A
    A --> P[Prompt augmenté]
    P --> L[LLM]
    L --> R[Réponse]
```

### Retrieval : évaluer et améliorer la pertinence

Le retrieval fournit des candidats. Le reranking peut améliorer leur ordre ; l'évaluation permet de vérifier si cela apporte un gain.

```mermaid
flowchart LR
    Q[Question] --> V[Vector search : Top 10]
    V --> R[Reranking]
    R --> K[Top 3 retenus]
    K --> L[Contexte du LLM]
```

### RAG modulaire : composer le pipeline Spring AI

`RetrievalAugmentationAdvisor` permet de composer les étapes de retrieval et de traitement des documents autour du modèle.

```mermaid
flowchart LR
    Q[Query] --> T[Query transformation]
    T --> D[DocumentRetriever]
    D --> P[DocumentPostProcessor]
    P --> A[Query augmentation]
    A --> L[LLM]
```

### Le RAG ne connaît pas notre commande

Connaître une politique de remboursement ne signifie pas connaître l'état d'une commande.

```mermaid
flowchart LR
    U[Demande CMD-1042] --> Q{Information nécessaire ?}
    Q -->|Politique générale| V[(VectorStore)]
    Q -->|État réel de CMD-1042| O[OrderService]
    V --> R[Connaissance documentaire]
    O --> D[Donnée transactionnelle autorisée]
```

### Tool Calling : orchestrer les appels aux services Java

Le modèle demande un Tool ; `ToolCallingAdvisor` orchestre la boucle ; `ToolCallingManager` résout et exécute les appels via les `ToolCallback`.

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    participant C as ChatClient (Spring Boot)
    participant A as ToolCallingAdvisor
    participant L as ChatModel / LLM
    participant M as ToolCallingManager
    participant T as ToolCallback / RefundReadTools
    participant S as OrderService

    U->>C: Analyser CMD-1042
    C->>A: Requête + Tools disponibles
    A->>L: Prompt + définitions des Tools
    L-->>A: ToolCall getOrder(CMD-1042)
    A->>M: Exécuter les appels demandés
    M->>T: Résoudre et invoquer getOrder
    T->>S: getAuthorizedOrder(userId, CMD-1042)
    S-->>T: Résultat autorisé ou refus
    T-->>M: Résultat du Tool
    M-->>A: ToolExecutionResult
    A->>L: Historique + résultat du Tool
    L-->>A: Réponse finale ou nouvel appel
    A-->>C: Réponse
    C-->>U: Résultat de l'application
```

### Déclarer un Tool avec Spring AI : @Tool et ToolContext

`@Tool` rend une méthode Java disponible au modèle ; `ToolContext` transmet des informations d'exécution fournies par l'application.

```mermaid
flowchart LR
    M[LLM : orderId] --> T[@Tool getOrder]
    S[Spring Security : utilisateur] --> C[ToolContext]
    C --> T
    T --> B[OrderService : ACL]
    B --> R[OrderView]
```

### Tool Calling : le modèle propose, l'application contrôle

Un Tool déclenche une demande d'opération ; l'identité, les autorisations et les décisions métier restent contrôlées par l'application.

```mermaid
flowchart TD
    U[Demande de remboursement CMD-1042] --> L[LLM / Tool Calling]
    L --> G[getOrder / checkRefundEligibility]
    G --> A{Accès autorisé ?}
    A -->|Non| X[Accès refusé]
    A -->|Oui| E[EligibilityService : décision Java]
    E -->|Non éligible| N[Aucune création]
    E -->|Éligible| C{Confirmation fiable côté application ?}
    C -->|Non| W[Attendre confirmation]
    C -->|Oui| R[Revalidation métier et création du brouillon]
```

### MCP Server : exposer des capacités distantes

Un serveur MCP publie des capacités accessibles à des applications clientes via un protocole standardisé.

```mermaid
flowchart LR
    subgraph SERVER["refund-mcp-server · Spring Boot · :8081"]
        P["RefundPolicyTools · @Tool"] --> S["RefundPolicyService"]
        S --> D["Politiques versionnées"]
        P --> CB["MethodToolCallbackProvider"]
        CB --> MS["MCP Server · /mcp"]
    end
    MS ---|"Streamable HTTP · MCP"| H["Applications clientes MCP"]
```

### MCP Client : découvrir et utiliser des Tools distants

Le client MCP découvre les capacités d'un serveur ; Spring AI les adapte en ToolCallbacks utilisables par ChatClient.

```mermaid
sequenceDiagram
    autonumber
    actor U as Utilisateur
    box Refund Management · MCP Host
        participant C as ChatClient
        participant A as ToolCallingAdvisor
        participant M as ToolCallingManager
        participant MC as MCP Client / ToolCallback
    end
    participant L as LLM
    participant S as refund-mcp-server

    Note over MC,S: Initialisation : découverte des Tools MCP
    MC->>S: tools/list
    S-->>MC: getRefundPolicy + schéma
    U->>C: Que dit REF-PROOF-001 ?
    C->>A: Prompt + callbacks MCP
    A->>L: Question + Tools disponibles
    L-->>A: Appeler getRefundPolicy
    A->>M: Déléguer l'exécution
    M->>MC: Appeler ToolCallback
    MC->>S: tools/call getRefundPolicy
    S-->>MC: Politique REF-PROOF-001
    MC-->>M: Résultat
    M-->>A: ToolExecutionResult
    A->>L: Contexte enrichi du résultat
    L-->>A: Réponse finale
    A-->>C: Réponse
    C-->>U: Explication
```

### Workflow ou agent : qui pilote l'exécution ?

Dans un workflow, **Java détermine les étapes** ; dans une boucle agentique, **le modèle choisit les Tools** disponibles au fil de son investigation.

```mermaid
flowchart LR
    U["Demande : vérifier CMD-1042"] --> D{Mode d'orchestration}
    subgraph W["WORKFLOW · ordre imposé par Spring Boot"]
        W1["Extraire RefundIntent"] --> W2["Charger commande autorisée"]
        W2 --> W3["Vérifier éligibilité"]
        W3 --> W4["Attendre confirmation / brouillon contrôlé"]
    end
    subgraph A["AGENT · exploration limitée en lecture"]
        A1["LLM : choisir un Tool"] --> A2["getOrder / checkRefundEligibility"]
        A2 --> A1
        A1 --> A3["Synthèse d'investigation"]
    end
    D --> W1
    D --> A1
```

### Workflows : orchestrer avec Java et Spring AI

Un workflow déterministe combine une étape IA avec des services Java, selon un ordre fixé par l'application.

```mermaid
flowchart LR
    I["Message naturel"] --> S["Spring AI : RefundIntent"]
    S --> O["OrderService + ACL"]
    O --> E["EligibilityService"]
    E --> C{"Éligible et confirmé ?"}
    C -->|Non| R["Refus / attente"]
    C -->|Oui| D["RefundService : brouillon"]
```

### Agentic Loop : orchestrer les Tools avec Spring AI

`ToolCallingAdvisor` orchestre la boucle LLM/Tools ; `ToolCallingManager` exécute les callbacks demandés.

```mermaid
flowchart LR
    C["ChatClient"] --> A["ToolCallingAdvisor"]
    A --> M["ChatModel / LLM"]
    M -->|"ToolCall"| A
    A -->|"Délègue"| G["ToolCallingManager"]
    G --> T["RefundReadTools · ToolCallback"]
    T --> S["Services métier + ACL"]
    S --> G
    G -->|"Résultat"| A
    A -->|"Contexte actualisé"| M
    M -->|"Sans nouveau ToolCall"| F["Réponse finale"]
```

### Choisir le bon niveau d'autonomie

L'autonomie dépend du type de tâche, du risque et des contrôles disponibles — pas de la seule capacité du modèle.

```mermaid
flowchart TD
    Q["Nouvelle tâche"] --> R{"Ordre métier imposé / écriture sensible ?"}
    R -->|Oui| W["Workflow Spring Boot"]
    R -->|Non| V{"Exploration variable utile ?"}
    V -->|Oui| A["Agentic Loop limitée"]
    V -->|Non| S["Appel direct / service Java"]
    W --> G["Contrôles métier et approbations"]
    A --> G
    S --> G
```

### Du POC à la production : quelles garanties ajouter ?

Un appel au modèle qui fonctionne ne garantit ni la sécurité, ni la qualité, ni la disponibilité du processus métier.

```mermaid
flowchart LR
    A["POC fonctionnel"] --> B["Sécurité"]
    B --> C["Observabilité"]
    C --> D["Évaluation"]
    D --> E["Résilience"]
    E --> F["Service exploitable"]
```

### Sécurité : authentifier l'utilisateur, autoriser chaque Tool

Spring Security établit l'identité ; le service métier applique l'autorisation. `ToolContext` transmet le contexte applicatif sans laisser le LLM choisir l'utilisateur.

```mermaid
flowchart LR
    U["Requête authentifiée"] --> S["Spring Security"]
    S --> C["ChatClient + ToolContext"]
    C --> T["@Tool getOrder"]
    T --> A{"OrderService : propriétaire autorisé ?"}
    A -->|Oui| O["OrderView"]
    A -->|Non| N["Refus d'accès"]
```

### Observability & Evaluation : voir l'exécution, mesurer la qualité

L'observabilité décrit **ce qui s'est passé** ; l'évaluation vérifie **si le résultat était bon**.

```mermaid
flowchart LR
    R["Interaction Spring AI"] --> O["Observabilité · Micrometer"]
    R --> E["Évaluation · jeux de tests"]
    O --> O1["Durées · erreurs · appels Tool"]
    E --> E1["Retrieval : Hit@K / MRR@K"]
    E --> E2["Réponse : pertinence / conformité métier"]
```

### Résilience : isoler les échecs de l'IA du métier

Timeout, limites d'exécution et réponse de dégradation empêchent une dépendance IA indisponible de provoquer une action métier non contrôlée.

```mermaid
flowchart TD
    U["Demande utilisateur"] --> G["SafeRefundAiService"]
    G --> T["AiExecutionGuard : délai / limites"]
    T --> A{"Interaction IA disponible ?"}
    A -->|Oui| R["Réponse IA contrôlée"]
    A -->|Non / délai dépassé| F["AI_UNAVAILABLE"]
    F --> S["État métier inchangé"]
```

### Spring AI : enrichir une architecture Spring Boot existante

ChatClient, RAG, Tools, MCP et orchestration s'intègrent aux services Java sans remplacer les responsabilités métier.

```mermaid
flowchart TB
    U["Utilisateur"] --> API["Refund API · Spring Boot"]
    API --> ORCH["Orchestration applicative"]

    subgraph AI["Capacités Spring AI"]
        C["ChatClient · Structured Output"]
        MEM["Advisors · Chat Memory"]
        RAG["RAG · VectorStore"]
        TOOLS["Tool Calling · ToolCallbacks"]
        MCP["MCP Host / Client"]
    end

    ORCH --> C
    C --> MEM
    ORCH --> RAG
    ORCH --> TOOLS
    ORCH --> MCP
    RAG --> PG[("Politiques · pgvector")]
    MCP <-->|"MCP"| MS["MCP Server · politiques"]
    TOOLS --> BIZ["Services Java · Order / Eligibility / Refund"]
    ORCH --> BIZ
    BIZ --> DB[("Commandes")]
    SEC["Spring Security · ACL"] -.-> API
    SEC -.-> BIZ
    OBS["Micrometer · Evaluation · Guard"] -.-> ORCH
```
