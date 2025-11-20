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
- **Reverse Proxy**: Caddy (Auto-HTTPS)
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

## Deployment

### Production mit Docker Compose

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

4. **Reverse Proxy** (Caddy) läuft automatisch mit Auto-HTTPS

### Backup & Recovery

Automatische Backups werden täglich erstellt:
```bash
# Manuelles Backup
docker compose exec postgres pg_dump -U stockstatus stockstatus > backup.sql

# Restore
docker compose exec -T postgres psql -U stockstatus stockstatus < backup.sql
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
