# Stock-Status Applikation - Detaillierte ToDo-Liste

Diese Liste beschreibt die schrittweise Umsetzung der Stock-Status Applikation basierend auf den Spezifikationen.

## Phase 1: Projekt-Setup & Infrastruktur ✅

### 1.1 Projekt-Struktur initialisieren ✅
- [x] Monorepo-Struktur erstellen mit Frontend- und Backend-Verzeichnissen
- [x] `.gitignore` Datei erstellen für Node, Java, Docker und IDE-spezifische Dateien
- [x] README.md mit Projektbeschreibung und Setup-Anleitung erstellen
- [x] `.env.example` Datei mit allen benötigten Umgebungsvariablen erstellen

### 1.2 Backend-Setup (Spring Boot) ✅
- [x] Spring Boot 3.x Projekt mit Maven/Gradle initialisieren
- [x] Dependencies hinzufügen: Spring Web, Spring Data JPA, PostgreSQL Driver, Lombok, Validation
- [x] Dependencies hinzufügen: Springdoc OpenAPI für API-Dokumentation
- [x] Dependencies hinzufügen: Apache POI für Excel-Import, OpenCSV für CSV-Import
- [x] Modulare Package-Struktur erstellen: `web`, `service`, `repository`, `config`, `domain`
- [x] `application.yml` mit Profilen (`local`, `prod`) konfigurieren
- [x] Logging-Konfiguration (Logback) mit JSON-Format einrichten

### 1.3 Frontend-Setup (Angular) ✅
- [x] Angular 21+ Projekt mit Standalone Components initialisieren
- [x] Angular Material installieren und Dark Theme konfigurieren
- [x] Chart.js für Visualisierungen installieren
- [x] Angular Router konfigurieren
- [x] ESLint und Prettier konfigurieren
- [x] Environment-Dateien für `development` und `production` erstellen
- [x] HTTP Interceptor für API-Aufrufe vorbereiten

### 1.4 Datenbank-Setup ✅
- [x] PostgreSQL Docker Container Konfiguration erstellen
- [x] Flyway für Datenbank-Migrationen einrichten
- [x] Erste Migration: Datenbank-Schema für Stocks-Tabelle erstellen
- [x] Migration für ETFs-Tabelle erstellen
- [x] Migration für ETF-Allocations-Tabelle erstellen (Verknüpfung ETF zu Stocks)
- [x] Migration für Portfolio-Tabelle erstellen
- [x] Seed-Skript für Beispiel-Länder und Branchen erstellen

### 1.5 Docker & Deployment ✅
- [x] Dockerfile für Spring Boot Backend erstellen
- [x] Dockerfile für Angular Frontend (Multi-stage mit NGINX) erstellen
- [x] `docker-compose.yml` für lokale Entwicklung erstellen (Frontend, Backend, DB)
- [x] `docker-compose.prod.yml` für Production mit Redis und Backup-Service erstellen
- [x] Caddyfile für Reverse Proxy mit TLS erstellen
- [x] Backup-Script für PostgreSQL `pg_dump` mit GPG-Verschlüsselung erstellen

## Phase 2: Backend - Domänenmodell & Basis-Entities ✅

### 2.1 Stock Entity & Repository ✅
- [x] `Stock` Entity erstellen mit Feldern: id, name, isin, country, sector, createdAt, updatedAt
- [x] Optimistic Locking (@Version) für Stock Entity hinzufügen
- [x] `StockRepository` Interface mit Spring Data JPA erstellen
- [x] Custom Query-Methoden hinzufügen: findByIsin, findByCountry, searchByName

### 2.2 ETF Entity & Repository ✅
- [x] `ETF` Entity erstellen mit Feldern: id, name, isin, importerType, createdAt, updatedAt
- [x] Enum `ImporterType` für verschiedene CSV/Excel-Importer erstellen
- [x] `ETFRepository` Interface erstellen
- [x] Custom Query-Methoden: findByIsin, findAll mit Pagination

### 2.3 ETF Allocation Entity & Repository ✅
- [x] `ETFAllocation` Entity erstellen mit Feldern: id, etfId, stockId, percentage, uploadDate, version
- [x] Bidirektionale Beziehung zu ETF und Stock definieren (@ManyToOne)
- [x] `ETFAllocationRepository` Interface erstellen
- [x] Query-Methoden: findByEtfId, findLatestByEtfId, deleteAllByEtfIdAndVersion

### 2.4 Portfolio Entity & Repository ✅
- [x] `Portfolio` Entity erstellen mit Feldern: id, userId (optional für zukünftige Multi-User), name
- [x] `PortfolioPosition` Entity erstellen: id, portfolioId, assetType (STOCK/ETF), assetId, quantity
- [x] `PortfolioRepository` und `PortfolioPositionRepository` erstellen
- [x] Query-Methoden für Portfolio-Positionen

## Phase 3: Backend - Business Logic & Services

### 3.1 Stock Service ✅
- [x] `StockService` Interface und Implementation erstellen
- [x] CRUD-Methoden implementieren: create, update, delete, findById, findAll
- [x] Validation: ISIN-Format prüfen, Duplikate verhindern
- [x] Exception-Handling: StockNotFoundException, DuplicateStockException

### 3.2 ETF Service ✅
- [x] `ETFService` Interface und Implementation erstellen
- [x] CRUD-Methoden für ETFs implementieren
- [x] Methode zum Abrufen der aktuellen Allocation eines ETFs
- [x] Methode zum Abrufen der Historie aller Allocations eines ETFs

### 3.3 File Import Service - Abstraktion ✅
- [x] `FileImporter` Interface definieren mit Methode: `List<AllocationEntry> parseFile(MultipartFile file)`
- [x] `AllocationEntry` DTO erstellen mit Feldern: isin, name, percentage
- [x] `ImporterFactory` Service erstellen zur Auswahl des richtigen Importers basierend auf ImporterType

### 3.4 File Import Service - Konkrete Importer ✅
- [x] `GenericCSVImporter` implementieren (CSV mit Spalten: ISIN, Name, Percentage)
- [x] `GenericExcelImporter` implementieren (Excel mit gleicher Struktur)
- [x] Validierung der Import-Daten: ISIN-Format, Percentage-Summe = 100%, keine negativen Werte
- [x] Fehlerbehandlung: InvalidFileFormatException, AllocationSumException

### 3.5 ETF Allocation Service ✅
- [x] `ETFAllocationService` erstellen
- [x] Methode zum Speichern einer neuen Allocation-Version aus Import
- [x] Automatisches Anlegen fehlender Stocks beim Import (mit Warnung)
- [x] Methode zum Abrufen der aktuellen Allocation
- [x] Methode zum Löschen alter Allocation-Versionen (Retention-Policy)

### 3.6 Portfolio Service ✅
- [x] `PortfolioService` Interface und Implementation erstellen
- [x] CRUD-Methoden für Portfolio und Positionen
- [x] Methode zur Berechnung der aggregierten Aktienallokation unter Berücksichtigung der ETF-Aufschlüsselung
- [x] Methode zur Berechnung der Länderallokation über alle Positionen
- [x] Methode zur Ermittlung der Top 20 Aktienwerte im Portfolio
- [x] DTO `PortfolioAnalysisDTO` erstellen für aggregierte Ergebnisse

### 3.7 Calculation Engine ✅
- [x] `PortfolioCalculationService` erstellen
- [x] Algorithmus: Alle Portfolio-Positionen durchlaufen
- [x] Für Stocks: Direkte Quantity übernehmen
- [x] Für ETFs: Quantity * ETF-Allocation-Percentage für jeden Stock berechnen
- [x] Aggregation aller Stock-Mengen (gleiche ISIN aufsummieren)
- [x] Länder-Gruppierung und Prozentberechnung

## Phase 4: Backend - REST API & Controller

### 4.1 Stock REST Controller ✅
- [x] `StockController` mit @RestController und @RequestMapping("/api/stocks") erstellen
- [x] POST `/api/stocks` - Stock erstellen
- [x] GET `/api/stocks` - Alle Stocks mit Pagination abrufen
- [x] GET `/api/stocks/{id}` - Stock nach ID abrufen
- [x] PUT `/api/stocks/{id}` - Stock aktualisieren
- [x] DELETE `/api/stocks/{id}` - Stock löschen
- [x] GET `/api/stocks/search?query=` - Stocks nach Name oder ISIN suchen
- [x] Request/Response DTOs erstellen für Stock-Operationen

### 4.2 ETF REST Controller ✅
- [x] `ETFController` mit @RequestMapping("/api/etfs") erstellen
- [x] POST `/api/etfs` - ETF erstellen
- [x] GET `/api/etfs` - Alle ETFs mit Pagination abrufen
- [x] GET `/api/etfs/{id}` - ETF Details abrufen
- [x] PUT `/api/etfs/{id}` - ETF aktualisieren
- [x] DELETE `/api/etfs/{id}` - ETF löschen
- [x] GET `/api/etfs/{id}/allocations` - Aktuelle Allocation abrufen
- [x] GET `/api/etfs/{id}/allocations/history` - Allocation-Historie abrufen
- [x] Request/Response DTOs erstellen

### 4.3 File Upload Controller ✅
- [x] `FileUploadController` mit @RequestMapping("/api/etfs/{id}/upload") erstellen
- [x] POST `/api/etfs/{id}/upload` - Multipart File Upload für Allocation
- [x] File-Type Validation (CSV, XLSX)
- [x] File-Size Limit konfigurieren (max 10MB)
- [x] Response: Import-Statistik (anzahl Einträge, neue Stocks, Fehler)
- [x] Error-Handling für invalide Dateien

### 4.4 Portfolio REST Controller ✅
- [x] `PortfolioController` mit @RequestMapping("/api/portfolios") erstellen
- [x] POST `/api/portfolios` - Portfolio erstellen
- [x] GET `/api/portfolios` - Alle Portfolios abrufen (mit Pagination)
- [x] GET `/api/portfolios/{id}` - Portfolio nach ID abrufen
- [x] GET `/api/portfolios/{id}/with-positions` - Portfolio mit allen Positionen
- [x] PUT `/api/portfolios/{id}` - Portfolio aktualisieren
- [x] DELETE `/api/portfolios/{id}` - Portfolio löschen
- [x] POST `/api/portfolios/{id}/positions` - Position hinzufügen
- [x] PUT `/api/portfolios/positions/{id}` - Position aktualisieren (Quantity ändern)
- [x] DELETE `/api/portfolios/positions/{id}` - Position entfernen
- [x] GET `/api/portfolios/{id}/positions` - Alle Positionen abrufen
- [x] GET `/api/portfolios/{id}/positions/stocks` - Nur Stock-Positionen
- [x] GET `/api/portfolios/{id}/positions/etfs` - Nur ETF-Positionen
- [x] Request/Response DTOs erstellen

### 4.5 Dashboard Controller ✅
- [x] `DashboardController` mit @RequestMapping("/api/dashboard") erstellen
- [x] GET `/api/dashboard/analysis/{portfolioId}` - Vollständige Portfolio-Analyse abrufen
- [x] Response: Top 20 Stocks, Länderallokation, Portfolio-Positionen
- [x] GET `/api/dashboard/country-allocation/{portfolioId}` - Länder-Verteilung als Chart-Daten
- [x] GET `/api/dashboard/top-stocks/{portfolioId}?limit=20` - Top Stock-Positionen
- [x] GET `/api/dashboard/stock-allocations/{portfolioId}` - Aggregierte Stock-Allokationen
- [ ] Caching für Dashboard-Daten mit Redis (optional)

### 4.6 OpenAPI Dokumentation ✅
- [x] Springdoc OpenAPI Configuration erstellen mit API Info und Tags
- [x] `/v3/api-docs` und Swagger UI unter `/swagger-ui.html` verfügbar machen
- [x] Gruppierung der Endpoints nach Stock, ETF, Portfolio, Dashboard, File Upload
- [x] OpenAPI Annotations zu StockController hinzufügen
- [ ] API-Beschreibungen und Beispiele für alle anderen Controller hinzufügen (optional)

### 4.7 Global Exception Handling ✅
- [x] `@RestControllerAdvice` für globales Exception-Handling erstellen
- [x] Standard-Error-Response DTO mit ValidationError Support definieren
- [x] Handler für: ResourceNotFoundException (404)
- [x] Handler für: DuplicateResourceException (409)
- [x] Handler für: InvalidFileFormatException (400)
- [x] Handler für: AllocationSumException (400)
- [x] Handler für: MethodArgumentNotValidException (400) mit Validation Errors
- [x] Handler für: MaxUploadSizeExceededException (413)
- [x] Handler für: IllegalArgumentException (400)
- [x] Handler für: Generic Exception (500)
- [x] HTTP Status Codes korrekt zuordnen

## Phase 5: Backend - Testing ✅

### 5.1 Unit Tests für Services ✅
- [x] Unit Tests für `StockService` mit Mockito (17 Tests)
- [x] Unit Tests für `ETFService` (20 Tests)
- [x] Unit Tests für `PortfolioCalculationService` mit verschiedenen Szenarien (5 Tests)
- [x] Test-Konfiguration mit H2 in-memory Datenbank
- [x] application-test.yml mit Test-Profil erstellt

### 5.2 Integration Tests ✅
- [x] BaseIntegrationTest Klasse für gemeinsame Test-Setup erstellt
- [x] H2 in-memory Datenbank für Tests konfiguriert
- [x] @SpringBootTest Integration Tests Setup
- [x] MockMvc für HTTP-Layer Tests konfiguriert

### 5.3 API Tests ✅
- [x] MockMvc Tests für `StockController` (7 Tests)
  - POST /api/stocks - Create stock
  - GET /api/stocks - List with pagination
  - GET /api/stocks/{id} - Get by ID
  - PUT /api/stocks/{id} - Update stock
  - DELETE /api/stocks/{id} - Delete stock
  - GET /api/stocks/search - Search stocks
  - Validation error tests
- [x] End-to-End Integration Test (2 Tests)
  - Complete workflow: Stock → ETF → Portfolio → Positions
  - CRUD operations test
- [x] **Alle 51 Tests erfolgreich** ✅

## Phase 6: Frontend - Basis-Setup & Routing

### 6.1 Komponenten-Struktur
- [ ] `CoreModule` für Singleton-Services und Guards erstellen (oder Standalone-Äquivalent)
- [ ] `SharedModule` für wiederverwendbare Komponenten und Pipes
- [ ] Ordnerstruktur: `features/`, `shared/`, `core/`, `models/`

### 6.2 Routing
- [ ] App-Router mit Routes konfigurieren
- [ ] Route `/dashboard` für Dashboard-Komponente
- [ ] Route `/stocks` für Stock-Liste
- [ ] Route `/stocks/create` für Stock-Formular
- [ ] Route `/stocks/:id/edit` für Stock-Bearbeitung
- [ ] Route `/etfs` für ETF-Liste
- [ ] Route `/etfs/create` für ETF-Formular
- [ ] Route `/etfs/:id` für ETF-Details und Allocation-Upload
- [ ] Route `/portfolio` für Portfolio-Verwaltung
- [ ] Lazy Loading für Feature-Module konfigurieren

### 6.3 API Service Layer
- [ ] `StockApiService` mit HttpClient für alle Stock-Endpoints erstellen
- [ ] `EtfApiService` für alle ETF-Endpoints
- [ ] `PortfolioApiService` für Portfolio-Endpoints
- [ ] `DashboardApiService` für Dashboard-Endpoints
- [ ] Error-Interceptor für HTTP-Fehlerbehandlung
- [ ] Loading-Interceptor für globale Ladeanzeige (optional)

### 6.4 State Management
- [ ] Angular Signals für Stock-State einrichten
- [ ] Signals für ETF-State
- [ ] Signals für Portfolio-State
- [ ] Signals für Dashboard-State
- [ ] Alternativ: NgRx Store Setup falls komplexere Anforderungen

### 6.5 Models & DTOs
- [ ] TypeScript Interfaces für Stock, ETF, Allocation, Portfolio, PortfolioPosition
- [ ] DTOs für API-Requests und Responses
- [ ] Enums für AssetType (STOCK, ETF), ImporterType

## Phase 7: Frontend - Shared Components & UI

### 7.1 Layout & Navigation
- [ ] Main-Layout-Komponente mit Toolbar und Sidenav erstellen
- [ ] Navigation-Menü mit Links zu Dashboard, Stocks, ETFs, Portfolio
- [ ] Dark Theme über Angular Material anwenden
- [ ] Responsive Layout mit Angular Flex Layout oder CSS Grid

### 7.2 Shared Components
- [ ] `DataTableComponent` für generische Tabellen mit Pagination und Sorting
- [ ] `ConfirmDialogComponent` für Lösch-Bestätigungen
- [ ] `LoadingSpinnerComponent` für Ladezustände
- [ ] `ErrorMessageComponent` für Fehleranzeigen
- [ ] `ChartComponent` Wrapper für Chart.js
- [ ] `FileUploadComponent` für Drag & Drop File-Upload

### 7.3 Pipes & Validators
- [ ] `CountryNamePipe` für Länder-Code zu Name
- [ ] `PercentagePipe` für Prozent-Formatierung
- [ ] Custom Validators: ISIN-Format, Positive-Number, Percentage-Range

## Phase 8: Frontend - Stock-Verwaltung

### 8.1 Stock-Liste
- [ ] `StockListComponent` erstellen
- [ ] Tabelle mit Spalten: Name, ISIN, Land, Branche, Aktionen
- [ ] Pagination und Sorting implementieren
- [ ] Suchfunktion für Name/ISIN
- [ ] Button zum Erstellen neuer Stocks
- [ ] Edit- und Delete-Icons pro Row

### 8.2 Stock-Formular
- [ ] `StockFormComponent` für Erstellen und Bearbeiten
- [ ] Reactive Form mit Feldern: Name, ISIN, Country (Dropdown), Sector (Dropdown)
- [ ] ISIN-Validierung
- [ ] Speichern-Button mit Success/Error-Feedback
- [ ] Cancel-Button zum Zurückkehren zur Liste

### 8.3 Stock-Löschung
- [ ] Confirm Dialog vor Löschung anzeigen
- [ ] Erfolgsmeldung nach Löschung
- [ ] Automatisches Aktualisieren der Liste

## Phase 9: Frontend - ETF-Verwaltung

### 9.1 ETF-Liste
- [ ] `EtfListComponent` erstellen
- [ ] Tabelle mit Spalten: Name, ISIN, Importer-Typ, Letzte Allocation, Aktionen
- [ ] Pagination und Sorting
- [ ] Button zum Erstellen neuer ETFs
- [ ] Link zu ETF-Details pro Row

### 9.2 ETF-Formular
- [ ] `EtfFormComponent` für Erstellen und Bearbeiten
- [ ] Reactive Form mit Feldern: Name, ISIN, ImporterType (Dropdown)
- [ ] ISIN-Validierung
- [ ] Speichern und Zurück zur Liste

### 9.3 ETF-Details & Allocation-Upload
- [ ] `EtfDetailComponent` erstellen
- [ ] ETF-Informationen anzeigen
- [ ] Sektion für File-Upload: Drag & Drop oder Browse
- [ ] Progress-Bar während Upload
- [ ] Import-Statistik nach erfolgreichem Upload anzeigen
- [ ] Tabelle mit aktueller Allocation (Stock-Name, ISIN, Percentage)
- [ ] Button zum Anzeigen der Historie

### 9.4 Allocation-Historie
- [ ] `AllocationHistoryComponent` als Dialog oder separate Route
- [ ] Liste aller Allocation-Versionen mit Upload-Datum
- [ ] Expandable Rows zum Anzeigen der Details jeder Version

## Phase 10: Frontend - Portfolio-Verwaltung

### 10.1 Portfolio-Übersicht
- [ ] `PortfolioComponent` erstellen
- [ ] Tabelle mit Portfolio-Positionen: Asset-Typ, Name, ISIN, Quantity, Aktionen
- [ ] Button zum Hinzufügen neuer Position
- [ ] Edit-Icon für Quantity-Änderung
- [ ] Delete-Icon zum Entfernen

### 10.2 Position hinzufügen/bearbeiten
- [ ] `PositionFormComponent` als Dialog
- [ ] Asset-Typ auswählen (STOCK oder ETF)
- [ ] Autocomplete-Dropdown für Asset-Auswahl basierend auf Typ
- [ ] Quantity-Eingabe mit Validierung (positive Zahl)
- [ ] Speichern und Dialog schließen

### 10.3 Position löschen
- [ ] Confirm Dialog vor Löschung
- [ ] Erfolgsmeldung und Tabellen-Update

## Phase 11: Frontend - Dashboard

### 11.1 Dashboard-Layout
- [ ] `DashboardComponent` erstellen
- [ ] Grid-Layout mit verschiedenen Cards/Panels

### 11.2 Portfolio-Zusammenfassung Card
- [ ] Card mit Portfolio-Positionen (Anzahl Stocks, ETFs)
- [ ] Gesamtanzahl unterschiedlicher Stocks nach Aufschlüsselung
- [ ] Optional: Gesamtwert (falls Preise integriert werden)

### 11.3 Top 20 Stocks Card
- [ ] Tabelle mit Top 20 Aktienwerten nach Quantity
- [ ] Spalten: Rank, Name, ISIN, Land, Aggregierte Quantity
- [ ] Sortierung nach Quantity

### 11.4 Länder-Allokation Card
- [ ] Pie Chart oder Bar Chart mit Länder-Verteilung
- [ ] Chart.js Integration
- [ ] Prozent-Anzeige pro Land
- [ ] Legende mit Farben

### 11.5 Branchen-Allokation Card (optional)
- [ ] Ähnlich wie Länder-Allokation für Branchen

### 11.6 Auto-Refresh
- [ ] Dashboard-Daten automatisch aktualisieren beim Navigieren
- [ ] Refresh-Button zum manuellen Neuladen

## Phase 12: Testing & Quality Assurance

### 12.1 Frontend Unit Tests
- [ ] Jasmine/Jest Tests für alle Services
- [ ] Tests für State-Management (Signals/NgRx)
- [ ] Tests für Pipes und Validators
- [ ] Tests für kritische Komponenten (Portfolio-Berechnung)

### 12.2 Frontend Component Tests
- [ ] Shallow Component Tests für alle Feature-Komponenten
- [ ] Tests für Formular-Validierungen
- [ ] Tests für User-Interaktionen (Button-Clicks, Form-Submit)

### 12.3 E2E Tests
- [ ] Cypress oder Playwright Setup
- [ ] E2E-Flow: Stock erstellen → ETF erstellen → Allocation hochladen
- [ ] E2E-Flow: Portfolio erstellen → Position hinzufügen → Dashboard prüfen
- [ ] E2E-Flow: Top 20 Stocks auf Dashboard prüfen

### 12.4 Code Quality
- [ ] ESLint und Prettier auf gesamte Codebase anwenden
- [ ] SonarQube oder ähnliches Tool zur Code-Qualitätsprüfung
- [ ] Pre-commit Hooks mit Husky für Lint und Tests

## Phase 13: CI/CD & Deployment

### 13.1 GitHub Actions / GitLab CI
- [ ] Pipeline-Datei erstellen (`.github/workflows/ci.yml` oder `.gitlab-ci.yml`)
- [ ] Stage: Frontend Lint und Tests
- [ ] Stage: Backend Lint und Tests (Maven/Gradle)
- [ ] Stage: Build Docker Images für Frontend und Backend
- [ ] Stage: Push Images zu Docker Registry (optional: GitHub Packages, Docker Hub)

### 13.2 Deployment-Automation
- [ ] Ansible Playbook für Deployment auf Server erstellen
- [ ] Playbook kopiert `docker-compose.prod.yml` auf Server
- [ ] Playbook führt `docker compose pull` und `docker compose up -d` aus
- [ ] Rollback-Playbook für vorherige Version

### 13.3 Backup & Recovery
- [ ] Backup-Cron-Job in Docker Compose testen
- [ ] Restore-Skript testen
- [ ] Dokumentation für Backup und Recovery

### 13.4 Monitoring
- [ ] Health Endpoints (`/actuator/health`) in Caddy/Traefik prüfen
- [ ] Prometheus-Metriken aktivieren und exposieren
- [ ] Optional: Grafana Dashboard für Metriken
- [ ] Log-Aggregation mit Loki oder ELK (optional)

## Phase 14: Dokumentation

### 14.1 API-Dokumentation
- [ ] OpenAPI/Swagger UI finalisieren und testen
- [ ] Beispiel-Requests und Responses dokumentieren

### 14.2 Entwickler-Dokumentation
- [ ] README.md mit Setup-Anleitung aktualisieren
- [ ] Architektur-Übersicht erstellen (Diagramm optional)
- [ ] Anleitung für lokales Setup (Docker Compose)
- [ ] Anleitung für Production-Deployment

### 14.3 User-Dokumentation
- [ ] Benutzerhandbuch erstellen (optional als separate Markdown-Datei)
- [ ] Screenshots der wichtigsten Views
- [ ] Anleitung zum Hochladen von Allocation-Dateien

### 14.4 Changelog
- [ ] `CHANGELOG.md` mit Versionierung und Release-Notes pflegen

## Phase 15: Optimierung & Polish

### 15.1 Performance-Optimierung
- [ ] Backend: Query-Optimierung und Indizes für häufige Queries
- [ ] Backend: Connection Pooling konfigurieren
- [ ] Frontend: Lazy Loading prüfen und optimieren
- [ ] Frontend: Bundle-Size analysieren und reduzieren (z.B. mit webpack-bundle-analyzer)

### 15.2 Security Hardening
- [ ] SQL Injection Prevention prüfen (Prepared Statements)
- [ ] XSS Prevention: Angular sanitized automatisch, aber Custom-HTML prüfen
- [ ] CSRF Protection aktivieren (Spring Security oder custom)
- [ ] Rate Limiting für API-Endpoints (optional mit Redis)
- [ ] CORS-Konfiguration prüfen

### 15.3 Accessibility
- [ ] ARIA-Labels für wichtige Buttons und Inputs
- [ ] Keyboard-Navigation testen
- [ ] Kontrast im Dark Theme prüfen (WCAG 2.1)

### 15.4 UX-Verbesserungen
- [ ] Loading States für alle API-Calls
- [ ] Toast-Notifications für Erfolg/Fehler-Meldungen
- [ ] Empty States für leere Listen
- [ ] Help-Tooltips für komplexe Felder (z.B. ISIN-Format)

## Phase 16: Launch-Vorbereitung

### 16.1 Final Testing
- [ ] Vollständiger Regressionstest aller Features
- [ ] Performance-Test mit größerer Datenmenge
- [ ] Security Audit (automatisiert mit OWASP ZAP oder manuell)

### 16.2 Production Setup
- [ ] Production-Datenbank initialisieren
- [ ] Seed-Daten für Länder und Branchen einspielen
- [ ] TLS-Zertifikat mit Let's Encrypt einrichten
- [ ] Backup-Strategie aktivieren und testen

### 16.3 Monitoring & Alerting
- [ ] Alerting für kritische Fehler einrichten (z.B. E-Mail bei 500 Errors)
- [ ] Uptime Monitoring (z.B. UptimeRobot, StatusCake)

### 16.4 Release
- [ ] Version Tag (v1.0.0) erstellen
- [ ] Release Notes im Changelog
- [ ] Docker Images mit Tag veröffentlichen
- [ ] Deployment auf Production-Server

---

## Hinweise zur Umsetzung

- **Iterative Entwicklung**: Die ToDos können in kleineren Batches umgesetzt werden. Es empfiehlt sich, zuerst Backend-Entities und Services zu implementieren, dann die REST API und parallel dazu das Frontend.
- **Testing**: Tests sollten parallel zur Entwicklung geschrieben werden, nicht erst am Ende.
- **Dokumentation**: OpenAPI und Code-Kommentare sollten während der Entwicklung gepflegt werden.
- **Docker**: Lokales Setup mit Docker Compose sollte früh verfügbar sein, um Integration zu testen.

**Geschätzte Anzahl der ToDos**: ~200+ einzelne Aufgaben
**Empfohlene Team-Größe**: 1-2 Full-Stack Entwickler
**Geschätzte Dauer**: 4-8 Wochen (abhängig von Erfahrung und Umfang)
