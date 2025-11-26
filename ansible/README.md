# Stock-Status Deployment Automation

Diese Ansible Playbooks ermöglichen das automatisierte Deployment, Rollback, Backup und Monitoring der Stock-Status Applikation.

## Voraussetzungen

- Ansible 2.9 oder höher installiert
- SSH-Zugriff zum Zielserver
- Docker und Docker Compose auf dem Zielserver installiert
- GitHub Container Registry Zugriff (für private Images)

## Installation

1. Ansible installieren:
```bash
# macOS
brew install ansible

# Ubuntu/Debian
sudo apt-get update
sudo apt-get install ansible

# Fedora/RHEL
sudo dnf install ansible
```

2. Ansible Docker Collection installieren:
```bash
ansible-galaxy collection install community.docker
```

## Konfiguration

### 1. Inventory anpassen

Bearbeiten Sie `inventory.yml` und passen Sie folgende Werte an:

```yaml
all:
  hosts:
    production:
      ansible_host: your-server.example.com  # Ihre Server-Adresse
      ansible_user: deploy                    # SSH-Benutzer
      ansible_ssh_private_key_file: ~/.ssh/id_rsa  # SSH-Key Pfad

  vars:
    docker_image_owner: your-github-username  # Ihr GitHub Username
```

### 2. Umgebungsvariablen setzen

Für private Docker Registries:

```bash
export DOCKER_REGISTRY_USERNAME="your-username"
export DOCKER_REGISTRY_PASSWORD="your-github-token"
```

### 3. .env Datei auf dem Server

Beim ersten Deployment wird `.env.example` als Vorlage kopiert. Passen Sie die `.env` Datei auf dem Server an:

```bash
ssh deploy@your-server.example.com
cd /opt/stock-status
nano .env
```

## Playbooks

### 1. deploy.yml - Hauptdeployment

Deployed die Applikation auf den Zielserver.

**Features:**
- Installiert Docker und Docker Compose (falls nicht vorhanden)
- Kopiert docker-compose.prod.yml und Konfigurationsdateien
- Erstellt automatisch Backup vor Deployment
- Pulled neueste Docker Images
- Startet die Applikation
- Führt Health Check durch
- Automatischer Rollback bei Fehler (optional)

**Verwendung:**

```bash
# Standard Deployment (latest Tag)
ansible-playbook -i inventory.yml deploy.yml

# Deployment mit spezifischem Tag
ansible-playbook -i inventory.yml deploy.yml -e "docker_image_tag=v1.2.0"

# Deployment mit Docker Registry Credentials
ansible-playbook -i inventory.yml deploy.yml \
  -e "docker_registry_username=${DOCKER_REGISTRY_USERNAME}" \
  -e "docker_registry_password=${DOCKER_REGISTRY_PASSWORD}"

# Deployment ohne automatischen Rollback
ansible-playbook -i inventory.yml deploy.yml -e "rollback_enabled=false"

# Deployment ohne Backup
ansible-playbook -i inventory.yml deploy.yml -e "backup_before_deploy=false"
```

**Variablen:**
- `docker_image_tag`: Docker Image Tag (default: latest)
- `backup_before_deploy`: Backup vor Deployment erstellen (default: true)
- `rollback_enabled`: Automatischer Rollback bei Fehler (default: true)
- `rolling_update`: Rolling Update ohne Downtime (default: false)

### 2. rollback.yml - Rollback zu vorheriger Version

Rollback zu einer vorherigen Applikationsversion.

**Features:**
- Stoppt aktuelle Applikation
- Stellt optional Datenbank-Backup wieder her
- Pulled vorherige Docker Images
- Startet Applikation mit alter Version
- Health Check der wiederhergestellten Version

**Verwendung:**

```bash
# Interaktives Rollback (fragt nach Tag und DB-Restore)
ansible-playbook -i inventory.yml rollback.yml

# Rollback zu spezifischer Version ohne Prompts
ansible-playbook -i inventory.yml rollback.yml \
  -e "rollback_tag=v1.1.0" \
  -e "restore_database=no"
```

**Variablen:**
- `rollback_tag`: Docker Image Tag für Rollback (wird interaktiv abgefragt)
- `restore_database`: Datenbank wiederherstellen (yes/no, wird interaktiv abgefragt)

### 3. backup.yml - Backup Management

Erstellt manuelle Backups und verwaltet die Backup-Retention.

**Features:**
- Erstellt manuelles Datenbank-Backup
- Optional GPG-Verschlüsselung
- Listet alle Backups auf
- Löscht alte Backups (älter als 14 Tage)

**Verwendung:**

```bash
# Backup erstellen
ansible-playbook -i inventory.yml backup.yml

# Backup mit GPG-Verschlüsselung
ansible-playbook -i inventory.yml backup.yml -e "gpg_recipient=backup@example.com"
```

**Backup-Dateien:**
- Speicherort: `/opt/stock-status/backups/`
- Format: `manual-YYYYMMDDTHHMMSS.dump`
- Retention: 14 Tage

### 4. monitoring.yml - Monitoring Setup

Richtet Health Checks und Monitoring auf dem Server ein.

**Features:**
- Erstellt Health Check Script
- Konfiguriert systemd Timer für regelmäßige Checks
- Prüft Backend, Frontend, Datenbank
- Zeigt Docker Container Status
- Überwacht Disk Space und DB-Größe

**Verwendung:**

```bash
# Monitoring Setup
ansible-playbook -i inventory.yml monitoring.yml

# Manueller Health Check auf dem Server
ssh deploy@your-server.example.com
/opt/stock-status/health-check.sh
```

**Health Check Komponenten:**
- Backend Health: `http://localhost:8080/actuator/health`
- Backend Info: `http://localhost:8080/actuator/info`
- Frontend: `http://localhost:80`
- Datenbank: `pg_isready`
- Docker Container Status
- Disk Usage
- Database Size

**Systemd Timer:**
- Service: `stock-status-health.service`
- Timer: `stock-status-health.timer`
- Intervall: Alle 5 Minuten

## Workflow-Beispiele

### Erstes Deployment

```bash
# 1. Inventory konfigurieren
nano ansible/inventory.yml

# 2. SSH-Zugriff testen
ssh deploy@your-server.example.com

# 3. Deployment durchführen
cd ansible
ansible-playbook -i inventory.yml deploy.yml

# 4. Monitoring einrichten
ansible-playbook -i inventory.yml monitoring.yml

# 5. Health Check prüfen
ssh deploy@your-server.example.com /opt/stock-status/health-check.sh
```

### Update auf neue Version

```bash
# 1. Backup erstellen
ansible-playbook -i inventory.yml backup.yml

# 2. Neues Deployment mit spezifischem Tag
ansible-playbook -i inventory.yml deploy.yml -e "docker_image_tag=v1.3.0"

# 3. Health Check prüfen
ssh deploy@your-server.example.com /opt/stock-status/health-check.sh
```

### Rollback nach fehlgeschlagenem Update

```bash
# Option 1: Automatischer Rollback (wenn aktiviert)
# Der deploy.yml Playbook führt automatisch Rollback durch bei Fehler

# Option 2: Manueller Rollback
ansible-playbook -i inventory.yml rollback.yml
# Eingabe: rollback_tag = v1.2.0
# Eingabe: restore_database = yes
```

### Regelmäßiges Backup

```bash
# Manuelles Backup
ansible-playbook -i inventory.yml backup.yml

# Oder als Cronjob auf dem Server (wird durch docker-compose.prod.yml gesteuert)
# Siehe backup-Service in docker-compose.prod.yml
```

## Troubleshooting

### SSH-Verbindung schlägt fehl

```bash
# SSH-Key prüfen
ssh -i ~/.ssh/id_rsa deploy@your-server.example.com

# SSH-Key Permissions korrigieren
chmod 600 ~/.ssh/id_rsa
```

### Docker Registry Authentifizierung schlägt fehl

```bash
# GitHub Token mit Packages-Berechtigung erstellen
# Settings -> Developer settings -> Personal access tokens -> Generate new token
# Scopes: read:packages, write:packages

# Token als Umgebungsvariable setzen
export DOCKER_REGISTRY_PASSWORD="ghp_your_token_here"
```

### Health Check schlägt fehl

```bash
# Logs prüfen
ssh deploy@your-server.example.com
cd /opt/stock-status
docker-compose logs -f backend
docker-compose logs -f frontend

# Container Status prüfen
docker ps -a
```

### Backup-Restore schlägt fehl

```bash
# Backup-Dateien prüfen
ssh deploy@your-server.example.com
ls -lh /opt/stock-status/backups/

# Manueller Restore
docker exec -i stock-status-db pg_restore -U stockstatus -d stockstatus -c -v < backup.dump
```

## Best Practices

1. **Vor jedem Production-Deployment:**
   - Backup erstellen
   - Health Check der aktuellen Version durchführen
   - Rollback-Plan bereit haben

2. **Versionierung:**
   - Verwenden Sie semantische Versionierung (SemVer)
   - Taggen Sie Releases in Git: `git tag v1.2.0`
   - Deployen Sie getaggte Versionen statt `latest`

3. **Monitoring:**
   - Richten Sie Health Check Timer ein
   - Überwachen Sie Logs regelmäßig
   - Setzen Sie Alerting für kritische Fehler auf

4. **Backup:**
   - Automatische Backups über Docker Compose
   - Manuelle Backups vor größeren Updates
   - Testen Sie Restore-Prozedur regelmäßig

5. **Security:**
   - Verwenden Sie SSH-Keys statt Passwörter
   - Speichern Sie Secrets nicht in Git
   - Nutzen Sie `.env` Dateien für sensitive Daten
   - Verschlüsseln Sie Backups mit GPG

## Weitere Informationen

- **CI/CD Pipeline:** `.github/workflows/ci.yml`
- **Docker Compose:** `docker-compose.prod.yml`
- **Datenbank Migrationen:** `backend/src/main/resources/db/migration/`
- **Health Endpoints:**
  - Backend: `http://localhost:8080/actuator/health`
  - Backend Info: `http://localhost:8080/actuator/info`
