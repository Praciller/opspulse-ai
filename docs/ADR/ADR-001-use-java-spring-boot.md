# ADR-001 — Use Java 21 + Spring Boot 3.x for the backend

- **Status:** Accepted
- **Date:** 2026-07-10
- **Review date:** 2026-10-10
- **Decision owner:** Portfolio project

## Context

OpsPulse-AI needs a backend that:
- Is widely recognized by hiring managers (portfolio value).
- Has a mature data/JPA/security/AI ecosystem.
- Runs within free-tier compute (< 512MB RAM, single CPU, idle-suspending).
- Supports Docker deployment and clean modular architecture.
- Integrates with a provider-agnostic AI client.

The team is a single contributor; productivity per line of code matters more than raw startup time, but memory ceiling is a hard constraint (NFR-1, NFR-9).

## Decision

Use **Java 21 (LTS) + Spring Boot 3.x with Spring Web MVC (servlet stack)**.

- Spring Boot 3.x requires Java 17+; Java 21 gives records, sealed classes, pattern matching, virtual threads preview.
- Spring Web MVC (not WebFlux) — servlet stack is simpler, lower cognitive overhead, and well-supported by Spring Security/JPA/OpenAPI/Testcontainers ecosystems.
- Enable virtual threads (`spring.threads.virtual.enabled=true`) for cheap IO concurrency without reactive complexity.

## Consequences

**Positive**
- Strong ecosystem: Spring Data JPA, Flyway, Testcontainers, Actuator, springdoc-openapi, Spring Security, Spring AI (ADR-007).
- High portfolio recognition; matches typical enterprise JDs.
- Mature testing story (JUnit 5, Mockito, Testcontainers).

**Negative**
- Heavier cold start than Quarkus/Micronaut/GraalVM native.
- Free-tier memory ceiling requires JVM tuning and slim Docker image (multi-stage build, JRE not JDK, `-XX:MaxRAMPercentage=75 -XX:+UseSerialGC`).
- Reactive patterns not idiomatic; virtual threads cover most IO concurrency needs.

**Neutral**
- Spring Boot release cadence requires minor version bumps every 6 months.

## Alternatives Considered

| Alternative | Why rejected |
|---|---|
| Quarkus + GraalVM native | Excellent startup/RAM, but native build complexity and smaller portfolio recognition for SME ops domain. Worth a Phase 8 spike. |
| Micronaut | Good startup, but smaller ecosystem and AI client choice is thinner. |
| Kotlin + Spring Boot | Adds language learning curve; portfolio targets Java roles. |
| Node.js + TypeScript | Strong on frontend reuse, but loses JPA/SQL rigor and Java portfolio signal. |
| Go | Excellent for free-tier, but loses Spring/JPA/Testcontainers portfolio value and slower to write CRUD-heavy domain. |

## Compliance & Validation

- Build: the accepted Gradle Kotlin DSL single-module build must pass with `./gradlew build` on the Java 21 toolchain.
- Runtime: hosted demo RSS < 512MB verified via Actuator `/actuator/metrics/jvm.memory.used` or Render metrics.
- Boot time: < 25s cold start on Render Free (target).

## References

- [ARCHITECTURE.md](../ARCHITECTURE.md)
- [ADR-007 — AI framework selection](ADR-007-ai-framework-selection-spring-ai-vs-langchain4j.md)
