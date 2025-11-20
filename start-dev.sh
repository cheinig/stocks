#!/bin/bash
# Development Startup Script
# Startet die Entwicklungsumgebung

set -e

# Wechsel ins Projektverzeichnis
cd "$(dirname "$0")"

echo "🚀 Stock-Status Development Setup"
echo "=================================="
echo ""

# Farben für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 1. PostgreSQL prüfen/starten
echo "📦 Schritt 1: PostgreSQL Container"
if ! docker ps | grep -q stock-status-db; then
    echo "${YELLOW}PostgreSQL läuft nicht. Starte Container...${NC}"
    docker compose down -v 2>/dev/null || true
    docker compose up -d postgres
    echo "${GREEN}✓ PostgreSQL gestartet${NC}"
    echo "Warte 10 Sekunden bis PostgreSQL bereit ist..."
    sleep 10
else
    echo "${GREEN}✓ PostgreSQL läuft bereits${NC}"
fi

# 2. Verbindung testen
echo ""
echo "🔌 Schritt 2: Datenbankverbindung testen"
if docker exec stock-status-db psql -U stockstatus -d stockstatus -c '\conninfo' > /dev/null 2>&1; then
    echo "${GREEN}✓ Datenbank erreichbar${NC}"
    echo "   User: stockstatus"
    echo "   Database: stockstatus"
    echo "   Password: dev123"
else
    echo "${RED}✗ Datenbankverbindung fehlgeschlagen${NC}"
    exit 1
fi

# 3. Backend-Konfiguration prüfen
echo ""
echo "⚙️  Schritt 3: Backend-Konfiguration prüfen"
if grep -q "dev123" backend/src/main/resources/application.yml; then
    echo "${GREEN}✓ application.yml ist korrekt konfiguriert${NC}"
else
    echo "${RED}✗ application.yml hat nicht das richtige Passwort${NC}"
    exit 1
fi

# 4. Anleitung
echo ""
echo "✅ Setup abgeschlossen! Du kannst jetzt starten:"
echo ""
echo "${GREEN}Terminal 1 - Backend:${NC}"
echo "  cd backend"
echo "  mvn spring-boot:run"
echo ""
echo "${GREEN}Terminal 2 - Frontend:${NC}"
echo "  cd frontend"
echo "  npm start"
echo ""
echo "Dann öffne:"
echo "  🌐 Frontend:  http://localhost:4200"
echo "  🔧 Backend:   http://localhost:8080"
echo "  📚 API Docs:  http://localhost:8080/swagger-ui.html"
echo ""
