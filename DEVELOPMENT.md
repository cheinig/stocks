# Development Guide

## Quick Start (ohne Docker)

### 1. PostgreSQL starten (nur DB im Docker)

```bash
docker compose up -d postgres
```

### 2. Backend starten

```bash
cd backend
mvn spring-boot:run
```

Das Backend verbindet sich automatisch mit:
- **URL**: `jdbc:postgresql://localhost:5432/stockstatus`
- **User**: `stockstatus`
- **Password**: `dev123`

Das Backend läuft auf http://localhost:8080

### 3. Frontend starten

```bash
cd frontend
npm install  # nur beim ersten Mal
npm start
```

Das Frontend läuft auf http://localhost:4200

## Nützliche Kommandos

### Datenbank

```bash
# Verbinde zur Datenbank
docker exec -it stock-status-db psql -U stockstatus -d stockstatus

# Zeige alle Tabellen
\dt

# Zeige Stocks
SELECT * FROM stocks;

# Reset Datenbank (ACHTUNG: Löscht alle Daten!)
docker compose down -v
docker compose up -d postgres
```

### Backend

```bash
# Kompilieren
mvn compile

# Tests ausführen
mvn test

# Package (JAR erstellen)
mvn package

# OpenAPI Docs
http://localhost:8080/swagger-ui.html
```

### Frontend

```bash
# Build für Production
npm run build

# Tests
npm test

# Linting
ng lint
```

## Konfiguration

### Automatische Defaults (keine Konfiguration nötig!)

Das Backend (`application.yml`) ist vorkonfiguriert mit:
- **Database URL**: `jdbc:postgresql://localhost:5432/stockstatus`
- **Database User**: `stockstatus`
- **Database Password**: `dev123`
- **Backend Port**: `8080`
- **CORS**: Erlaubt `localhost:4200`

### Custom-Konfiguration (optional)

Falls du andere Werte verwenden möchtest:

```bash
# Via Umgebungsvariablen
export SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/mydb
export SPRING_DATASOURCE_USERNAME=myuser
export SPRING_DATASOURCE_PASSWORD=mypass

# Oder via Maven
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.password=mypass"
```

## Datenbank Schema

Flyway Migrationen befinden sich in:
```
backend/src/main/resources/db/migration/
```

Neue Migration erstellen:
```
V006__description.sql
```

## API Testing

### Mit curl

```bash
# Health Check
curl http://localhost:8080/actuator/health

# Get all stocks
curl http://localhost:8080/api/stocks

# Create stock
curl -X POST http://localhost:8080/api/stocks \
  -H "Content-Type: application/json" \
  -d '{"name":"Test AG","isin":"DE0000000001","country":"DE","sector":"Technology"}'
```

### Mit HTTPie

```bash
# Install
brew install httpie

# Get stocks
http localhost:8080/api/stocks
```

## IDE Setup

### IntelliJ IDEA

1. **Import Project**: File → Open → wähle `pom.xml` im backend-Ordner
2. **Lombok Plugin**: Installiere Lombok Plugin und aktiviere Annotation Processing
3. **Angular**: Öffne `frontend` als separates Projekt oder nutze das Terminal

### VS Code

Backend:
- Extension: "Language Support for Java"
- Extension: "Spring Boot Extension Pack"

Frontend:
- Extension: "Angular Language Service"
- Extension: "ESLint"
- Extension: "Prettier"

## Troubleshooting

### Port 8080 bereits belegt

```bash
# Finde Prozess
lsof -i :8080

# Töte Prozess
kill -9 <PID>
```

### Flyway Migration fehlgeschlagen

```bash
# Repariere Flyway
docker exec stock-status-db psql -U stockstatus -d stockstatus -c "DELETE FROM flyway_schema_history WHERE success = false;"

# Oder: Datenbank komplett neu
docker compose down -v
docker compose up -d postgres
```

### Frontend kann Backend nicht erreichen

1. Prüfe ob Backend läuft: http://localhost:8080/actuator/health
2. Prüfe Browser Console auf CORS-Fehler
3. Prüfe `ALLOWED_ORIGINS` in Backend application.yml

## Hot Reload

- **Frontend**: Läuft automatisch mit `npm start`
- **Backend**: Spring Boot DevTools aktiviert (automatischer Reload bei Änderungen)

## Seed-Daten

Die Datenbank wird automatisch mit Beispiel-Daten gefüllt:
- 16 Stocks (Apple, Microsoft, SAP, etc.)
- 3 ETFs
- ETF Allocations

Siehe: `backend/src/main/resources/db/migration/V005__seed_example_data.sql`
