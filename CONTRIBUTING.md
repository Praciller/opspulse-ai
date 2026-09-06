# Contributing

## Prerequisites

- Java 21
- Docker Desktop for PostgreSQL-backed integration tests

## Verify changes

```bash
./gradlew check --no-daemon
docker compose config
```

Use `gradlew.bat` instead of `./gradlew` on Windows.

## Project rules

- Keep domain code independent of Spring, JPA, Jackson, API, and infrastructure packages.
- Add Flyway migrations for schema changes; never use Hibernate schema generation in production.
- Keep secrets in environment variables and update `.env.example` with placeholders only.
- Add behavior-focused tests for changed public interfaces.
