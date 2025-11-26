# Container Build & Registry Guide

Diese Dokumentation beschreibt den automatischen Container-Build-Prozess und die Nutzung der GitHub Container Registry.

## Übersicht

Die CI/CD-Pipeline baut automatisch Docker-Container für Frontend und Backend und lädt diese in die GitHub Container Registry (ghcr.io) hoch.

## Automatischer Build-Prozess

### Trigger

Container werden automatisch gebaut bei:
- **Push auf `main` Branch** → `latest` Tag + Branch-Tag
- **Push auf `dev` Branch** → `dev` Tag + Branch-Tag
- **Push von Git Tags** (z.B. `v1.0.0`) → Versionierte Tags
- **Manueller Workflow-Dispatch** → Auf Anforderung

### Build-Pipeline

```
┌─────────────────────────────────────────────────────┐
│  1. Tests & Linting                                 │
│     ├─ Frontend: ESLint + Jest/Karma                │
│     └─ Backend: Maven Verify + JUnit                │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  2. Multi-Stage Docker Build                        │
│     ├─ Frontend: Node 20 → NGINX Alpine             │
│     └─ Backend: Maven + JDK 21 → JRE Alpine         │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  3. Multi-Platform Build                            │
│     ├─ linux/amd64 (x86_64)                         │
│     └─ linux/arm64 (ARM64/Apple Silicon)            │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  4. Push to GitHub Container Registry               │
│     └─ ghcr.io/<username>/stock-status-*            │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│  5. Security Scan                                   │
│     ├─ Trivy Vulnerability Scanner                  │
│     └─ Upload to GitHub Security                    │
└─────────────────────────────────────────────────────┘
```

## Container-Images

### Image-Namen

```
Frontend: ghcr.io/<username>/stock-status-frontend
Backend:  ghcr.io/<username>/stock-status-backend
```

Ersetzen Sie `<username>` mit Ihrem GitHub-Benutzernamen oder Organisation.

### Image-Tags

| Tag-Typ | Beispiel | Beschreibung |
|---------|----------|--------------|
| Branch | `main`, `dev` | Branch-Name |
| Latest | `latest` | Nur für main Branch |
| Version | `v1.0.0`, `v1.0`, `v1` | Semantic Versioning |
| SHA | `main-sha-abc123` | Branch + Git SHA |

#### Beispiele:

**Push auf `main` Branch:**
```
ghcr.io/username/stock-status-frontend:main
ghcr.io/username/stock-status-frontend:latest
ghcr.io/username/stock-status-frontend:main-sha-abc123
```

**Push von Git Tag `v1.2.3`:**
```
ghcr.io/username/stock-status-frontend:v1.2.3
ghcr.io/username/stock-status-frontend:v1.2
ghcr.io/username/stock-status-frontend:v1
```

**Push auf `dev` Branch:**
```
ghcr.io/username/stock-status-frontend:dev
ghcr.io/username/stock-status-frontend:dev-sha-xyz789
```

## Container Registry Zugriff

### Öffentliche vs. Private Images

**Standard:** Images sind privat und erfordern Authentifizierung.

**Öffentlich machen:**
1. Gehen Sie zu: `https://github.com/users/<username>/packages/container/stock-status-frontend/settings`
2. Wählen Sie "Change visibility" → "Public"
3. Wiederholen für Backend-Image

### Authentifizierung

#### GitHub Personal Access Token erstellen

1. Gehen Sie zu: `https://github.com/settings/tokens`
2. Klicken Sie auf "Generate new token (classic)"
3. Scopes auswählen:
   - `read:packages` (Images pullen)
   - `write:packages` (Images pushen - nur für Entwickler)
   - `delete:packages` (Images löschen - optional)
4. Token kopieren und sicher speichern

#### Docker Login

```bash
# Mit Token
echo $GITHUB_TOKEN | docker login ghcr.io -u <username> --password-stdin

# Oder interaktiv
docker login ghcr.io -u <username>
# Password: <paste your token>
```

#### Kubernetes Image Pull Secret

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<username> \
  --docker-password=<token> \
  --namespace=stock-status
```

Fügen Sie in Deployments hinzu:
```yaml
spec:
  imagePullSecrets:
    - name: ghcr-secret
```

## Images Verwenden

### Docker Pull

```bash
# Latest Version (main branch)
docker pull ghcr.io/<username>/stock-status-frontend:latest
docker pull ghcr.io/<username>/stock-status-backend:latest

# Spezifische Version
docker pull ghcr.io/<username>/stock-status-frontend:v1.0.0
docker pull ghcr.io/<username>/stock-status-backend:v1.0.0

# Dev Version
docker pull ghcr.io/<username>/stock-status-frontend:dev
docker pull ghcr.io/<username>/stock-status-backend:dev
```

### Docker Compose

```yaml
version: '3.8'

services:
  frontend:
    image: ghcr.io/<username>/stock-status-frontend:latest
    ports:
      - "80:80"

  backend:
    image: ghcr.io/<username>/stock-status-backend:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/stockstatus
```

### Kubernetes

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: frontend
spec:
  template:
    spec:
      containers:
        - name: frontend
          image: ghcr.io/<username>/stock-status-frontend:v1.0.0
          imagePullPolicy: Always
```

## Versionierung & Releases

### Semantic Versioning

Verwenden Sie Git Tags für Releases:

```bash
# Version Tag erstellen
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0

# Pipeline baut automatisch:
# - ghcr.io/<username>/stock-status-frontend:v1.0.0
# - ghcr.io/<username>/stock-status-frontend:v1.0
# - ghcr.io/<username>/stock-status-frontend:v1
```

### Release-Workflow

1. **Code fertigstellen:**
   ```bash
   git checkout main
   git pull origin main
   ```

2. **Version taggen:**
   ```bash
   git tag -a v1.0.0 -m "Release v1.0.0 - Initial release"
   git push origin v1.0.0
   ```

3. **Pipeline überwachen:**
   - Gehen Sie zu: `Actions` Tab im Repository
   - Warten Sie auf erfolgreichen Build

4. **Images verifizieren:**
   ```bash
   docker pull ghcr.io/<username>/stock-status-frontend:v1.0.0
   docker pull ghcr.io/<username>/stock-status-backend:v1.0.0
   ```

5. **Deployment:**
   ```bash
   # Kubernetes
   cd k8s
   # Anpassen: overlays/prod/kustomization.yaml
   # images.newTag: v1.0.0
   kubectl apply -k overlays/prod/

   # Oder Ansible
   cd ansible
   ansible-playbook -i inventory.yml deploy.yml -e "docker_image_tag=v1.0.0"
   ```

## Build-Optimierungen

### Layer Caching

Die Pipeline nutzt GitHub Actions Cache für schnellere Builds:

- **Frontend:** `npm ci` Dependencies werden gecached
- **Backend:** Maven Dependencies werden gecached
- **Docker:** Build-Layer werden zwischen Runs gecached

### Multi-Platform Support

Images werden für beide Architekturen gebaut:
- **linux/amd64** - Intel/AMD (Standard Server)
- **linux/arm64** - ARM (Apple Silicon, AWS Graviton)

Dies ermöglicht Deployment auf allen gängigen Plattformen.

### Build-Args

Die Pipeline setzt automatisch:
```dockerfile
BUILD_DATE=<commit timestamp>
VCS_REF=<git sha>
```

Diese können in Dockerfiles genutzt werden:
```dockerfile
ARG BUILD_DATE
ARG VCS_REF
LABEL build-date=$BUILD_DATE
LABEL vcs-ref=$VCS_REF
```

## Image-Metadaten

Jedes Image enthält OCI-konforme Labels:

```bash
docker inspect ghcr.io/<username>/stock-status-frontend:latest

# Labels:
# - org.opencontainers.image.title
# - org.opencontainers.image.description
# - org.opencontainers.image.version
# - org.opencontainers.image.created
# - org.opencontainers.image.source
# - org.opencontainers.image.revision
```

## Security Scanning

### Automatische Scans

Nach jedem Build werden Images mit Trivy gescannt:
- Vulnerabilities in Base Images
- Vulnerabilities in Dependencies
- Results in GitHub Security Tab

### Scan-Ergebnisse einsehen

1. Gehen Sie zu: `Security` → `Code scanning alerts`
2. Filter: `trivy-frontend` oder `trivy-backend`
3. Beheben Sie kritische Vulnerabilities

### Manueller Scan

```bash
# Trivy installieren
brew install trivy

# Image scannen
trivy image ghcr.io/<username>/stock-status-frontend:latest

# Nur HIGH und CRITICAL
trivy image --severity HIGH,CRITICAL ghcr.io/<username>/stock-status-frontend:latest
```

## Image-Verwaltung

### Images auflisten

```bash
# Via GitHub CLI
gh api /user/packages/container/stock-status-frontend/versions

# Via Docker
docker search ghcr.io/<username>/stock-status
```

### Images löschen

**Via GitHub Web UI:**
1. Gehen Sie zu: `Packages` → Select Package → `Package settings`
2. Wählen Sie Version
3. `Delete this version`

**Via GitHub CLI:**
```bash
gh api -X DELETE /user/packages/container/stock-status-frontend/versions/<version-id>
```

**Achtung:** In Kubernetes genutzte Images nicht löschen!

### Image Retention

**Empfohlene Retention-Policy:**
- `latest` - Immer behalten
- Versionierte Tags (`v*`) - Mindestens 3 letzte Versionen
- Branch Tags - 30 Tage
- SHA Tags - 7 Tage

## Troubleshooting

### Build schlägt fehl

**Frontend Build Error:**
```bash
# Lokal reproduzieren
cd frontend
docker build -t test-frontend .

# Logs prüfen
docker build --progress=plain -t test-frontend .
```

**Backend Build Error:**
```bash
# Lokal reproduzieren
cd backend
docker build -t test-backend .

# Maven Dependencies prüfen
./mvnw dependency:tree
```

### Push fehlgeschlagen

**Fehler:** `denied: permission_denied`

**Lösung:**
1. Prüfen Sie GitHub Token Permissions
2. Scope `write:packages` muss aktiviert sein
3. Repository muss Token-Zugriff erlauben

### Image Pull schlägt fehl

**Fehler:** `unauthorized: authentication required`

**Lösung:**
```bash
# Neu einloggen
docker logout ghcr.io
docker login ghcr.io -u <username>

# Token-Permissions prüfen
# Mindestens: read:packages
```

**Fehler:** `manifest unknown`

**Lösung:**
- Tag existiert nicht oder wurde gelöscht
- Image ist privat und Sie sind nicht authentifiziert
- Typo im Image-Namen

### Cache-Probleme

**Symptom:** Builds sind sehr langsam

**Lösung:**
```bash
# Cache invalidieren (in GitHub Actions)
# Workflow-Datei bearbeiten und force push

# Oder in Workflow-File:
# cache-from: type=gha
# ändern zu:
# cache-from: type=registry,ref=ghcr.io/<username>/stock-status-frontend:buildcache
```

## Best Practices

### 1. Image-Tags

✅ **DO:**
- Verwenden Sie semantische Versionierung
- Taggen Sie Releases explizit
- Nutzen Sie `latest` nur für `main` branch

❌ **DON'T:**
- Nutzen Sie `latest` nicht in Production
- Überschreiben Sie keine Version-Tags
- Löschen Sie keine in Production genutzten Images

### 2. Security

✅ **DO:**
- Prüfen Sie Scan-Ergebnisse regelmäßig
- Updaten Sie Base Images
- Nutzen Sie minimal Images (Alpine)
- Non-root User in Containern

❌ **DON'T:**
- Ignorieren Sie keine kritischen Vulnerabilities
- Committen Sie keine Secrets
- Nutzen Sie keine veralteten Base Images

### 3. Build-Optimierung

✅ **DO:**
- Nutzen Sie Multi-Stage Builds
- Cachen Sie Dependencies
- Minimieren Sie Layer-Anzahl
- Nutzen Sie `.dockerignore`

❌ **DON'T:**
- Kopieren Sie nicht unnötige Dateien
- Installieren Sie keine Dev-Dependencies im Final Image
- Bauen Sie nicht ohne Cache

## Weitere Ressourcen

- [GitHub Container Registry Docs](https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-container-registry)
- [Docker Best Practices](https://docs.docker.com/develop/dev-best-practices/)
- [OCI Image Spec](https://github.com/opencontainers/image-spec)
- [Trivy Documentation](https://aquasecurity.github.io/trivy/)
