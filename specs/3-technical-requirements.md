## 7. Technische Überlegungen (Diskussionsbasis)

### 7.1 Architektur & Schnittstellen
- Single-Page-Application + RESTful Backend

### 7.2 Frontend (Angular + TypeScript)
- Angular 21+ mit Standalone Components, Routing über Angular Router; State-Management via Angular Signals oder NgRx (falls komplexere Zustände).
- UI-Library: Angular Material oder Tailwind CSS + Angular CDK; Nur ein Dark Mode soll erstellt werden.
- Charts/Visualisierung: charts.js für Kennzahlen und Zeitreihen
- Formulare: Reactive Forms + Zod oder Angular Validators;.
- Internationalisierung optional über Angular i18n oder Transloco.

### 7.3 Backend (Spring Boot + Java)
- Spring Boot 3.x mit modularer Struktur: `web` (Controller), `service` (Domänenlogik), `repository` (Persistenz), `config`.
- Persistenz: Spring Data JPA über Hibernate; Entities für Streams, Assets, Liabilities, Revisions, SzenarioOverrides.
- API-Spezifikation via Springdoc OpenAPI; Tests: JUnit 5 + Testcontainers für Integration, MockMvc für API-Tests.

### 7.4 Persistenz & Datenhaltung
- Datenbank: PostgreSQL (JSONB für flexible Felder); Liquibase oder Flyway für Migrationen; Seeding-Skripte für Kategorien/Basiskonfiguration.
- ORM-Mapping entspricht Datenmodell (Stocks, ETFs, Allocations, Portfolio); Entities nutzen Optimistic Locking zur Konfliktvermeidung.
- Backups: `pg_dump` per Cronjob; Verschlüsselung der Dumps (gpg/age) und verifizierte Restore-Skripte.

### 7.5 Deployment & Betrieb
- Docker Compose Stack: `angular-frontend` (nginx serve), `spring-backend`, `postgres`, optional `redis` (Cache) und `backup`-Service.
- Reverse Proxy: Caddy oder Traefik übernimmt TLS, Weiterleitung und optionale Basic Auth.
- Observability: Logging im JSON-Format (Spring Logging + Logback), Health Endpoint `/actuator/health`, Prometheus-kompatible Metriken via `/actuator/prometheus`; optional Loki/Grafana.
- Config-Management: `.env` bzw. Spring Boot `application.yml` (Profile `local`, `prod`); Secrets via `.env`, Docker Secrets oder Vault.

### 7.6 Tooling & Dev Experience
- Monorepo (Angular + Spring Boot) mit pnpm/npm für Frontend, Maven/Gradle für Backend; Skripte zum parallelen Start (`npm run start:all`).
- Testing: Jasmine/Karma oder Jest für Angular Unit-Tests, Cypress/Playwright für kritische E2E-Flows; Backend mit JUnit/Testcontainers.
- Storybook für Angular-Komponenten (Dark-Variante); Pre-commit Hooks (Husky) für Linting (ESLint), Formatierung (Prettier) und Type-Checks.
- CI/CD-Pipeline (GitHub Actions, GitLab CI) führt Lint, Tests, Build und Docker Image Build aus; optional Deploy auf Selbst-Hosting-Ziel via Ansible.

### 7.7 Infrastruktur & Deployment
- **Docker Compose (prod.yml)**: 
  - `frontend` (Angular build, NGINX), 
  - `backend` (Spring Boot, Port 8080), 
  - `db` (Postgres 15 mit Volumen `pgdata`), 
  - `redis` (optional für Cache), 
  - `backup` (Cron-Container führt `pg_dump` aus).
- Secrets via `.env` (lokal) und Docker Secrets (`POSTGRES_PASSWORD`, `APP_SECRET`, `SESSION_SECRET`).
- Backup-Speicher: lokales verschlüsseltes Volume (`backups/`), Retention 14 Tage.
- Recovery-Prozedur: `docker compose down`, Restore mit `pg_restore`, `docker compose up`.
- Reverse Proxy (Caddyfile Beispiel): Auto-HTTPS mit Let’s Encrypt, Weiterleitung `/api`→Backend, `/`→Frontend.

### 7.8 Monitoring & Alerting
- Health Endpoints: `/actuator/health`, `/actuator/info`.
- Logs: Strukturierte JSON-Logs (Backend) mit Level, timestamp, userId (falls vorhanden); Frontend-Fehler können im UI als Admin-Hinweis angezeigt werden.

### 7.9 Migration & Datenpflege
- Schemaänderungen über Flyway (SQL-basierte Migrationen, Versionierung `VYYYYMMDDHHMM__desc.sql`).
- Rollback-Strategie: Für kritische Migrationen begleitende `undo`-Scripts; Backup vor Ausführung verpflichtend.
- Seed-Daten: `R__seed_categories.sql` initialisiert Standard-Kategorien, Standardrollen.
- Wartungsmodus: Flag in `application.yml` (`maintenance.enabled=true`) zeigt Frontend-Hinweis „Nur Lesen“.

### 7.10 Konfiguration & Anpassbarkeit
- `.env` / `application.yml` Parameter:
  - `APP_BASE_URL`, `SESSION_SECRET`, `MAIL_SMTP`, `ALLOWED_ORIGINS`, `BACKUP_CRON`, `ALERT_THRESHOLD_NET_WORTH`.
- Speicherorte für Konfig: Versionskontrolle (Beispiel `.env.example`), sensitives nur lokal/Secret-Store.

### 7.11 Deploy- & Release-Prozess
- Branching: `main` (stable), `dev` (Integration), Feature-Branches (`feature/...`); PR-Review Pflicht.
- Versionierung: SemVer (`v1.0.0`); Release-Notes in `CHANGELOG.md`.
- CI-Pipeline (GitHub Actions):
  1. Lint & Test Frontend (npm run lint/test).
  2. Lint & Test Backend (./mvnw verify).
  3. Build Docker Images, push zu Registry.
- Release: Tag auf `main`, CI erzeugt Release-Artefakte (Docker Compose Bundle). Deployment via `ansible-playbook deploy.yml` (kopiert Compose-Dateien, führt Rolling Restart aus).
- Rollback: Deployment-Playbook kann vorherigen Tag (per `docker compose pull <tag> && up -d`) wiederherstellen; Datenbank-Restore aus jüngstem Backup dokumentiert.

