# Deployment Guide

Dieser Leitfaden beschreibt den Deployment-Prozess für die Stock-Status Applikation.

## Übersicht

Die Applikation nutzt einen modernen CI/CD-Workflow:

1. **Continuous Integration** via GitHub Actions
2. **Container Registry** via GitHub Container Registry (ghcr.io)
3. **Deployment Automation** via Ansible
4. **Health Monitoring** via Systemd Timer

## CI/CD Pipeline

### Trigger

Die Pipeline wird ausgelöst durch:
- Push auf `main` oder `dev` Branch
- Pull Requests auf `main` oder `dev`
- Git Tags (z.B. `v1.0.0`)
- Manueller Workflow-Dispatch

### Container-Build

**Automatischer Build und Push zu GitHub Container Registry:**

Bei jedem Push werden Docker-Container automatisch gebaut und hochgeladen:
- **Frontend:** `ghcr.io/<username>/stock-status-frontend`
- **Backend:** `ghcr.io/<username>/stock-status-backend`

**Unterstützte Plattformen:**
- `linux/amd64` (Intel/AMD)
- `linux/arm64` (ARM/Apple Silicon)

Siehe [CONTAINER-BUILD.md](CONTAINER-BUILD.md) für Details zum Container-Build-Prozess.

### Pipeline-Stages

```
┌─────────────────────────────────────────────────────┐
│  1. Frontend Lint & Test                            │
│     ├─ ESLint                                       │
│     ├─ Unit Tests (Karma/Jasmine)                   │
│     └─ Code Coverage → Codecov                      │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  2. Backend Lint & Test                             │
│     ├─ Maven Verify                                 │
│     ├─ JUnit Tests                                  │
│     └─ Code Coverage → Codecov                      │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  3. Build & Push Docker Images (nur bei Push)       │
│     ├─ Build Frontend Image                         │
│     ├─ Build Backend Image                          │
│     └─ Push zu ghcr.io                              │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  4. Security Scan (nur bei Push)                    │
│     ├─ Trivy Vulnerability Scanner                  │
│     └─ SARIF Upload → GitHub Security               │
└─────────────────────────────────────────────────────┘
```

### Docker Image Tags

Automatisch erstellte Tags:
- `latest` - Nur für main Branch
- `main` / `dev` - Branch-Name
- `main-sha-abc123` - Branch + Git SHA
- `v1.2.3` - Semantic Version Tags
- `1.2` - Major.Minor Version

## Deployment

### Voraussetzungen

1. **Server-Setup:**
   ```bash
   # Docker installieren (Ubuntu/Debian)
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker $USER

   # Ansible installieren (lokaler Rechner)
   pip install ansible
   ansible-galaxy collection install community.docker
   ```

2. **SSH-Zugriff konfigurieren:**
   ```bash
   ssh-copy-id deploy@your-server.com
   ```

3. **GitHub Container Registry Zugriff:**
   ```bash
   # GitHub Personal Access Token erstellen mit 'read:packages' Scope
   # Settings → Developer settings → Personal access tokens

   export DOCKER_REGISTRY_USERNAME="your-github-username"
   export DOCKER_REGISTRY_PASSWORD="ghp_your_token_here"
   ```

### Deployment-Workflow

#### 1. Initiales Deployment

```bash
# 1. Inventory konfigurieren
cd ansible
cp inventory.yml inventory.prod.yml
nano inventory.prod.yml  # Server-Details anpassen

# 2. .env auf Server vorbereiten (einmalig)
ssh deploy@your-server.com
mkdir -p /opt/stock-status
nano /opt/stock-status/.env  # Siehe .env.example

# 3. Deployment ausführen
ansible-playbook -i inventory.prod.yml deploy.yml \
  -e "docker_registry_username=${DOCKER_REGISTRY_USERNAME}" \
  -e "docker_registry_password=${DOCKER_REGISTRY_PASSWORD}"

# 4. Monitoring einrichten
ansible-playbook -i inventory.prod.yml monitoring.yml

# 5. Health Check
ssh deploy@your-server.com /opt/stock-status/health-check.sh
```

#### 2. Update auf neue Version

```bash
# Option A: Mit spezifischer Version
ansible-playbook -i inventory.prod.yml deploy.yml \
  -e "docker_image_tag=v1.2.0" \
  -e "docker_registry_username=${DOCKER_REGISTRY_USERNAME}" \
  -e "docker_registry_password=${DOCKER_REGISTRY_PASSWORD}"

# Option B: Latest Version
ansible-playbook -i inventory.prod.yml deploy.yml \
  -e "docker_registry_username=${DOCKER_REGISTRY_USERNAME}" \
  -e "docker_registry_password=${DOCKER_REGISTRY_PASSWORD}"
```

#### 3. Rollback

```bash
# Interaktiv (fragt nach Version)
ansible-playbook -i inventory.prod.yml rollback.yml

# Mit spezifischer Version
ansible-playbook -i inventory.prod.yml rollback.yml \
  -e "rollback_tag=v1.1.0" \
  -e "restore_database=no"
```

## Backup & Recovery

### Automatische Backups

Konfiguriert in `docker-compose.prod.yml`:
- **Zeitpunkt**: Täglich um 02:00 Uhr
- **Retention**: 14 Tage
- **Speicherort**: `/opt/stock-status/backups/`

### Manuelle Backups

```bash
# Via Ansible
ansible-playbook -i inventory.prod.yml backup.yml

# Via Docker (auf dem Server)
docker compose exec postgres pg_dump -U stockstatus -F c stockstatus > backup.dump
```

### Restore

```bash
# Via Ansible (während Rollback)
ansible-playbook -i inventory.prod.yml rollback.yml
# → Auswahl: restore_database=yes

# Manuell (auf dem Server)
docker compose exec -T postgres pg_restore -U stockstatus -d stockstatus -c < backup.dump
```

## Monitoring

### Health Checks

Nach Installation von `monitoring.yml` läuft ein systemd Timer:

```bash
# Status prüfen
systemctl status stock-status-health.timer
systemctl status stock-status-health.service

# Logs anzeigen
journalctl -u stock-status-health.service -f

# Manuell ausführen
/opt/stock-status/health-check.sh
```

### Geprüfte Komponenten

- ✓ Backend Health (`/actuator/health`)
- ✓ Backend Info (`/actuator/info`)
- ✓ Frontend Erreichbarkeit
- ✓ Datenbank (`pg_isready`)
- ✓ Docker Container Status
- ✓ Disk Usage
- ✓ Database Size

### Log-Zugriff

```bash
# Alle Logs
docker compose logs -f

# Spezifischer Service
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f postgres

# Mit Zeitstempel und Limit
docker compose logs -f --tail=100 --timestamps backend
```

## Troubleshooting

### Pipeline schlägt fehl

**Frontend Tests:**
```bash
# Lokal reproduzieren
cd frontend
npm ci
npm run lint
npm test -- --watch=false
```

**Backend Tests:**
```bash
# Lokal reproduzieren
cd backend
./mvnw clean verify
```

### Deployment schlägt fehl

**SSH-Verbindung:**
```bash
# Connection testen
ansible all -i inventory.prod.yml -m ping

# Verbose Output
ansible-playbook -i inventory.prod.yml deploy.yml -vvv
```

**Docker Registry Auth:**
```bash
# Token-Berechtigung prüfen
curl -H "Authorization: token ${DOCKER_REGISTRY_PASSWORD}" \
  https://ghcr.io/v2/

# Manuell auf Server einloggen
ssh deploy@your-server.com
docker login ghcr.io -u your-username
```

### Health Check schlägt fehl

```bash
# Backend Health
curl http://localhost:8080/actuator/health

# Container Status
docker ps -a

# Backend Logs
docker compose logs --tail=100 backend

# Neustart
docker compose restart backend
```

### Rollback schlägt fehl

```bash
# Verfügbare Images prüfen
docker images | grep stock-status

# Manueller Rollback
cd /opt/stock-status
docker compose down
# docker-compose.yml anpassen (IMAGE_TAG ändern)
docker compose pull
docker compose up -d
```

## Best Practices

### Release-Prozess

1. **Feature-Entwicklung auf Feature-Branch**
   ```bash
   git checkout -b feature/new-feature
   # ... entwickeln ...
   git push origin feature/new-feature
   ```

2. **Pull Request erstellen**
   - CI Pipeline läuft automatisch
   - Code Review durchführen
   - Merge nach `dev`

3. **Testing auf Dev-Umgebung**
   ```bash
   # Automatisch durch Push auf dev
   # Oder manuell:
   ansible-playbook -i inventory.dev.yml deploy.yml -e "docker_image_tag=dev"
   ```

4. **Release vorbereiten**
   ```bash
   git checkout main
   git merge dev
   git tag v1.2.0
   git push origin main --tags
   ```

5. **Production Deployment**
   ```bash
   # Backup erstellen
   ansible-playbook -i inventory.prod.yml backup.yml

   # Deployment
   ansible-playbook -i inventory.prod.yml deploy.yml -e "docker_image_tag=v1.2.0"

   # Health Check
   ssh deploy@production /opt/stock-status/health-check.sh
   ```

### Sicherheit

- **Niemals Secrets in Git committen**
- **`.env` Dateien nur lokal auf Servern**
- **GitHub Tokens mit minimalen Permissions (read:packages)**
- **SSH-Keys statt Passwörter**
- **Regelmäßige Security Scans beachten**

### Monitoring

- **Health Checks regelmäßig prüfen**
- **GitHub Security Alerts aktivieren**
- **Logs bei Problemen analysieren**
- **Backup-Retention überwachen**

## Weitere Ressourcen

- [Ansible Playbooks](../ansible/README.md)
- [Docker Compose Production](../docker-compose.prod.yml)
- [GitHub Actions Workflow](./workflows/ci.yml)
- [Projekt-Dokumentation](../README.md)
