# P0-A — Slide → Code → Test → Preuve

| Slide | Concept | Source | Test | Statut |
|---|---|---|---|---|
| 2–3 | Spring Boot existant | `refund-application/OrderService.java` | ApplicationSmokeTest | Core vérifié avec javac |
| 7 | Validation métier indépendante du LLM | `refund-application/EligibilityService.java` | ApplicationSmokeTest | Core vérifié avec javac |
| 18 | Contrôle d'autorisation | `refund-application/OrderService#getAuthorized` | ApplicationSmokeTest | Core vérifié avec javac |
| 22 | Workflow déterministe | `refund-application/RefundService#prepareDraft` | ApplicationSmokeTest | Core vérifié avec javac |
