# Stock-Status Applikation

Eine Web-Applikation zur Verwaltung und Analyse von Aktien- und ETF-Portfolios mit automatischer Aufschlüsselung der ETF-Zusammensetzung.

## Überblick

Die Stock-Status Applikation bietet:
- Verwaltung von Aktien (ISIN, Land, Branche)
- Verwaltung von ETFs mit CSV/Excel-Import für Asset-Allokation
- Portfolio-Management mit automatischer Berechnung der tatsächlichen Aktienallokation
- Dashboard mit Top 20 Aktienwerten und Länder-Allokation
- Historische Tracking von ETF-Zusammensetzungen

## Technologie-Stack

### Backend
- **Framework**: Spring Boot 3.x
- **Sprache**: Java 17+
- **Datenbank**: PostgreSQL 15
- **ORM**: Spring Data JPA / Hibernate
- **Migration**: Flyway
- **API-Dokumentation**: Springdoc OpenAPI
- **Build-Tool**: Maven

### Frontend
- **Framework**: Angular 21+
- **UI-Library**: Angular Material (Dark Theme)
- **Charts**: Chart.js
- **Sprache**: TypeScript
- **Styling**: SCSS + Angular Material Theming

### Infrastructure
- **Containerization**: Docker & Docker Compose
- **Orchestration**: Kubernetes (mit Kustomize)
- **Reverse Proxy**: Caddy (Docker) / NGINX Ingress (Kubernetes)
- **Caching**: Redis (optional)

## Projekt-Struktur

```
stock-status/
├── backend/                 # Spring Boot Backend
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/stockstatus/
│   │   │   │   ├── web/           # REST Controllers
│   │   │   │   ├── service/       # Business Logic
│   │   │   │   ├── repository/    # Data Access
│   │   │   │   ├── domain/        # Entities & DTOs
│   │   │   │   └── config/        # Configuration
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       └── db/migration/  # Flyway Scripts
│   │   └── test/
│   └── pom.xml
├── frontend/                # Angular Frontend
│   ├── src/
│   │   ├── app/
│   │   │   ├── features/    # Feature Modules
│   │   │   ├── shared/      # Shared Components
│   │   │   └── core/        # Core Services
│   │   └── environments/
│   └── package.json
├── docker-compose.yml       # Development Setup
├── docker-compose.prod.yml  # Production Setup
├── Caddyfile               # Reverse Proxy Config
└── README.md
```

## Voraussetzungen

- **Docker** und **Docker Compose** (empfohlen für einfachstes Setup)
- Alternativ für lokale Entwicklung:
  - Java 17+ und Maven
  - Node.js 18+ und npm/pnpm
  - PostgreSQL 15

## Setup & Installation

### Schnellstart mit Docker Compose

Die Entwicklungsumgebung ist out-of-the-box konfiguriert. Einfach starten:

1. **Services starten**:
   ```bash
   docker compose up -d
   ```

   Die .env Datei wird automatisch mit Development-Defaults verwendet:
   - User: `stockstatus`
   - Password: `dev123`
   - Database: `stockstatus`

2. **Applikation öffnen**:
   - Frontend: http://localhost:4200
   - Backend API: http://localhost:8080
   - API Dokumentation: http://localhost:8080/swagger-ui.html

3. **Logs anzeigen**:
   ```bash
   docker compose logs -f
   ```

4. **Services stoppen**:
   ```bash
   docker compose down
   ```

### Lokale Entwicklung

#### Backend

1. **PostgreSQL starten**:
   ```bash
   docker compose up -d postgres
   ```

2. **Backend bauen und starten**:
   ```bash
   cd backend
   ./mvnw clean install
   ./mvnw spring-boot:run
   ```

#### Frontend

1. **Dependencies installieren**:
   ```bash
   cd frontend
   npm install
   ```

2. **Entwicklungsserver starten**:
   ```bash
   npm start
   ```

3. **Öffne**: http://localhost:4200

## Datenbank-Migrationen

Flyway führt Migrationen automatisch beim Start aus. Migrations-Dateien befinden sich in:
```
backend/src/main/resources/db/migration/
```

Namenskonvention: `V{version}__{description}.sql`
Beispiel: `V001__create_stocks_table.sql`

## API-Dokumentation

Nach dem Start ist die OpenAPI-Dokumentation verfügbar:
- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs

## Testing

### Backend Tests
```bash
cd backend
./mvnw test                    # Unit Tests
./mvnw verify                  # Integration Tests
```

### Frontend Tests
```bash
cd frontend
npm test                       # Unit Tests
npm run e2e                    # E2E Tests (Cypress)
```

## CI/CD Pipeline

Die Applikation verwendet GitHub Actions für Continuous Integration und Deployment.

### Workflow Stages

1. **Frontend Lint & Test**
   - ESLint Code-Analyse
   - Unit Tests mit Karma/Jasmine
   - Code Coverage Upload

2. **Backend Lint & Test**
   - Maven Tests (Unit & Integration)
   - Code Coverage Upload
   - JaCoCo Reports

3. **Docker Image Build**
   - Automatischer Build bei Push auf `main` oder `dev`
   - Multi-Stage Docker Builds
   - Images werden zu GitHub Container Registry gepusht
   - Tags: `latest`, Branch-Name, SHA

4. **Security Scan**
   - Trivy Vulnerability Scanner
   - SARIF Reports zu GitHub Security

### Docker Images

Images sind verfügbar unter:
```
ghcr.io/<username>/stock-status-frontend:latest
ghcr.io/<username>/stock-status-backend:latest
```

Pull Images:
```bash
docker pull ghcr.io/<username>/stock-status-frontend:latest
docker pull ghcr.io/<username>/stock-status-backend:latest
```

## Deployment

Die Applikation kann auf verschiedene Arten deployed werden:

### 1. Kubernetes Deployment (Empfohlen für Production)

Vollständige Kubernetes-Deskriptoren mit Kustomize für Dev und Production.

**Quick Start:**
```bash
cd k8s
./deploy.sh dev   # Development
./deploy.sh prod  # Production
```

**Features:**
- StatefulSet für PostgreSQL mit persistentem Storage
- HorizontalPodAutoscaler für automatisches Skalieren
- NGINX Ingress mit TLS (cert-manager)
- Automatische Backups via CronJob
- Separate Overlays für Dev/Prod
- Health Checks und Readiness Probes

Siehe [k8s/README.md](k8s/README.md) und [k8s/QUICKSTART.md](k8s/QUICKSTART.md) für Details.

### 2. Automatisches Deployment mit Ansible

Für VM-basierte Production-Deployments mit Docker Compose.

Siehe [ansible/README.md](ansible/README.md) für Details.

**Schnellstart:**

```bash
# Inventory konfigurieren
cd ansible
nano inventory.yml

# Deployment durchführen
ansible-playbook -i inventory.yml deploy.yml

# Monitoring einrichten
ansible-playbook -i inventory.yml monitoring.yml
```

**Verfügbare Playbooks:**
- `deploy.yml` - Hauptdeployment mit Health Checks
- `rollback.yml` - Rollback zu vorheriger Version
- `backup.yml` - Backup Management
- `monitoring.yml` - Health Check Setup

### 3. Manuelles Production-Deployment mit Docker Compose

1. **Production Config**:
   ```bash
   cp .env.example .env
   # Setze sichere Passwörter und Production-URLs
   ```

2. **Images bauen**:
   ```bash
   docker compose -f docker-compose.prod.yml build
   ```

3. **Services starten**:
   ```bash
   docker compose -f docker-compose.prod.yml up -d
   ```

4. **Health Check**:
   ```bash
   curl http://localhost:8080/actuator/health
   ```

5. **Reverse Proxy** (Caddy) läuft automatisch mit Auto-HTTPS

### Backup & Recovery

#### Automatische Backups

Der `backup`-Service in `docker-compose.prod.yml` erstellt täglich verschlüsselte Backups:
- **Zeitpunkt**: 02:00 Uhr täglich
- **Retention**: 14 Tage
- **Verschlüsselung**: GPG (optional)

#### Manuelle Backups

```bash
# Mit Ansible
ansible-playbook -i ansible/inventory.yml ansible/backup.yml

# Manuell mit Docker
docker compose exec postgres pg_dump -U stockstatus -F c stockstatus > backup.dump

# Restore
docker compose exec -T postgres pg_restore -U stockstatus -d stockstatus -c < backup.dump
```

## Monitoring & Health Checks

### Health Endpoints

Die Applikation bietet folgende Health Endpoints:

- **Backend Health**: `http://localhost:8080/actuator/health`
- **Backend Info**: `http://localhost:8080/actuator/info`
- **Prometheus Metrics**: `http://localhost:8080/actuator/prometheus` (optional)

### Automatisches Monitoring

Mit Ansible können Sie automatische Health Checks einrichten:

```bash
ansible-playbook -i ansible/inventory.yml ansible/monitoring.yml
```

Dies erstellt:
- Health Check Script auf dem Server
- Systemd Timer für regelmäßige Checks (alle 5 Minuten)
- Überwachung von Backend, Frontend, Datenbank und Container Status

### Manueller Health Check

```bash
# Auf dem Server
/opt/stock-status/health-check.sh

# Remote via Ansible
ssh deploy@your-server.example.com /opt/stock-status/health-check.sh
```

### Logs

```bash
# Alle Services
docker compose logs -f

# Nur Backend
docker compose logs -f backend

# Nur Frontend
docker compose logs -f frontend

# Mit Zeitstempel und Zeilen-Limit
docker compose logs -f --tail=100 --timestamps backend
```

## Konfiguration

Wichtige Konfigurationsparameter in `.env`:

| Variable | Beschreibung | Default |
|----------|--------------|---------|
| `POSTGRES_PASSWORD` | PostgreSQL Passwort | - |
| `SPRING_DATASOURCE_URL` | Datenbank URL | jdbc:postgresql://postgres:5432/stockstatus |
| `SERVER_PORT` | Backend Port | 8080 |
| `ALLOWED_ORIGINS` | CORS Origins | http://localhost:4200 |

## Features

### 1. Aktien-Verwaltung
- CRUD-Operationen für Aktien
- Felder: Name, ISIN, Land, Branche
- Suche und Filter-Funktionen

### 2. ETF-Verwaltung
- CRUD-Operationen für ETFs
- CSV/Excel-Upload für Asset-Allokation
- Historische Tracking von Zusammensetzungen
- Unterstützte Importer: Generic CSV, Generic Excel

### 3. Portfolio-Management
- Hinzufügen von Aktien und ETFs zum Portfolio
- Angabe der Anteile pro Position
- Automatische Berechnung der tatsächlichen Aktienallokation
- ETFs werden in ihre Bestandteile aufgeschlüsselt

### 4. Dashboard & Analytics
- Top 20 Aktienwerte (aggregiert über ETFs)
- Länder-Allokation (Pie/Bar Chart)
- Portfolio-Übersicht
- Branchen-Verteilung (optional)

## Entwicklung

### Code-Style
- **Backend**: Google Java Style Guide
- **Frontend**: Angular Style Guide
- Formatter: Prettier für Frontend
- Linting: ESLint (Frontend), Checkstyle (Backend)

### Pre-commit Hooks
```bash
# Installiere Husky
cd frontend
npm run prepare
```

### Branching-Strategie
- `main`: Stable Release
- `dev`: Integration Branch
- `feature/*`: Feature-Branches

## Troubleshooting

### Port bereits belegt
```bash
# Prüfe belegte Ports
lsof -i :8080
lsof -i :4200

# Ändere Ports in .env oder docker-compose.yml
```

### Datenbank-Verbindung fehlgeschlagen
```bash
# Prüfe ob PostgreSQL läuft
docker compose ps postgres

# Prüfe Logs
docker compose logs postgres
```

### Frontend kann Backend nicht erreichen
- Prüfe CORS-Konfiguration in `application.yml`
- Prüfe `ALLOWED_ORIGINS` in `.env`

## Lizenz

Dieses Projekt ist für private Nutzung bestimmt.

## Kontakt & Support

Für Fragen und Issues siehe `specs/` Verzeichnis oder erstelle ein GitHub Issue.

---

**Version**: 1.0.0
**Letzte Aktualisierung**: 2025-11-20
