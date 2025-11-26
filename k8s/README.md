# Kubernetes Deployment für Stock-Status

Diese Dokumentation beschreibt das Deployment der Stock-Status Applikation in einem Kubernetes Cluster.

## Übersicht

Die Applikation besteht aus folgenden Komponenten:

- **PostgreSQL StatefulSet** - Datenbank mit persistentem Speicher
- **Backend Deployment** - Spring Boot REST API (2 Replicas)
- **Frontend Deployment** - Angular SPA mit NGINX (2 Replicas)
- **Ingress** - NGINX Ingress Controller mit TLS
- **HorizontalPodAutoscaler** - Automatisches Skalieren bei Last
- **CronJob** - Tägliche Datenbank-Backups

## Voraussetzungen

### 1. Kubernetes Cluster

Einen funktionierenden Kubernetes Cluster (Version 1.24+):

- **Lokal**: minikube, kind, k3s, Docker Desktop
- **Cloud**: GKE, EKS, AKS, DigitalOcean Kubernetes
- **On-Premise**: kubeadm, Rancher, OpenShift

### 2. kubectl

```bash
# Installation (macOS)
brew install kubectl

# Installation (Linux)
curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

# Cluster-Verbindung prüfen
kubectl cluster-info
kubectl get nodes
```

### 3. Kustomize

```bash
# Kustomize ist in kubectl >= 1.14 integriert
kubectl kustomize --help

# Oder standalone installieren
brew install kustomize  # macOS
```

### 4. NGINX Ingress Controller

```bash
# Installation (Standard)
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/cloud/deploy.yaml

# Für Minikube
minikube addons enable ingress

# Für kind
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# Status prüfen
kubectl get pods -n ingress-nginx
```

### 5. cert-manager (optional, für TLS)

```bash
# Installation
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# ClusterIssuer für Let's Encrypt erstellen
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
---
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-staging
spec:
  acme:
    server: https://acme-staging-v02.api.letsencrypt.org/directory
    email: your-email@example.com
    privateKeySecretRef:
      name: letsencrypt-staging
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

### 6. Metrics Server (für HPA)

```bash
# Installation
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml

# Für Minikube
minikube addons enable metrics-server

# Status prüfen
kubectl top nodes
kubectl top pods -A
```

## Deployment

### Schritt 1: Repository klonen und Konfiguration anpassen

```bash
cd k8s
```

**Wichtige Anpassungen:**

1. **Image-Namen in `base/backend-deployment.yaml` und `base/frontend-deployment.yaml`:**
   ```yaml
   image: ghcr.io/your-username/stock-status-backend:latest
   image: ghcr.io/your-username/stock-status-frontend:latest
   ```

2. **Domain in `base/ingress.yaml` und Overlays:**
   ```yaml
   host: stock-status.example.com  # Ihre Domain
   ```

3. **Secrets in `base/secret.yaml`:**
   ```bash
   # NIEMALS Passwörter in Git committen!
   # Stattdessen Secrets via kubectl erstellen:
   kubectl create secret generic stock-status-secrets \
     --namespace=stock-status \
     --from-literal=POSTGRES_PASSWORD='your-secure-password' \
     --from-literal=SPRING_DATASOURCE_PASSWORD='your-secure-password'
   ```

### Schritt 2: Development Deployment

```bash
# Preview der generierten Manifeste
kubectl kustomize overlays/dev/

# Deployment durchführen
kubectl apply -k overlays/dev/

# Status prüfen
kubectl get all -n stock-status-dev
kubectl get pvc -n stock-status-dev

# Logs anzeigen
kubectl logs -n stock-status-dev -l app=backend -f
kubectl logs -n stock-status-dev -l app=frontend -f
kubectl logs -n stock-status-dev -l app=postgres -f
```

**Port-Forwarding für lokalen Zugriff:**

```bash
# Backend
kubectl port-forward -n stock-status-dev svc/dev-backend-service 8080:8080

# Frontend
kubectl port-forward -n stock-status-dev svc/dev-frontend-service 8080:80

# PostgreSQL
kubectl port-forward -n stock-status-dev svc/dev-postgres-service 5432:5432

# Zugriff:
# Frontend: http://localhost:8080
# Backend API: http://localhost:8080/api
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Schritt 3: Production Deployment

```bash
# 1. Secrets erstellen (falls noch nicht vorhanden)
kubectl create namespace stock-status

kubectl create secret generic stock-status-secrets \
  --namespace=stock-status \
  --from-literal=POSTGRES_PASSWORD='your-very-secure-password' \
  --from-literal=SPRING_DATASOURCE_PASSWORD='your-very-secure-password'

# 2. ConfigMap und andere Anpassungen prüfen
kubectl kustomize overlays/prod/

# 3. Deployment durchführen
kubectl apply -k overlays/prod/

# 4. Status prüfen
kubectl get all -n stock-status
kubectl get pvc -n stock-status
kubectl get ingress -n stock-status

# 5. Pods überwachen
kubectl get pods -n stock-status -w

# 6. Logs prüfen
kubectl logs -n stock-status -l app=backend --tail=100 -f
```

### Schritt 4: DNS-Konfiguration

Für Production benötigen Sie einen DNS A-Record:

```
# Ingress IP ermitteln
kubectl get ingress -n stock-status stock-status-ingress

# DNS A-Record erstellen:
# stock-status.example.com -> <EXTERNAL-IP>
```

Für **Cloud Load Balancer** (GKE, EKS, AKS):
```bash
kubectl get svc -n ingress-nginx ingress-nginx-controller
```

## Struktur

```
k8s/
├── base/                           # Base Kustomize Konfiguration
│   ├── namespace.yaml              # Namespace Definition
│   ├── configmap.yaml              # Umgebungsvariablen
│   ├── secret.yaml                 # Secrets (Template, nicht für Production!)
│   ├── postgres-pvc.yaml           # PersistentVolumeClaims
│   ├── postgres-statefulset.yaml   # PostgreSQL StatefulSet
│   ├── backend-deployment.yaml     # Backend Deployment & Service
│   ├── frontend-deployment.yaml    # Frontend Deployment & Service
│   ├── ingress.yaml                # Ingress Controller Config
│   ├── hpa.yaml                    # HorizontalPodAutoscaler
│   ├── cronjob-backup.yaml         # Backup CronJob
│   └── kustomization.yaml          # Kustomize Base
├── overlays/
│   ├── dev/                        # Development Overlay
│   │   ├── kustomization.yaml
│   │   ├── patch-configmap.yaml    # Dev-spezifische Config
│   │   ├── patch-ingress.yaml      # Dev-Domain
│   │   └── patch-resources.yaml    # Reduzierte Resources
│   └── prod/                       # Production Overlay
│       ├── kustomization.yaml
│       ├── patch-configmap.yaml    # Prod-spezifische Config
│       └── patch-ingress.yaml      # Prod-Domain mit Rate Limiting
└── README.md
```

## Ressourcen & Skalierung

### Resource Limits

**Backend (Production):**
- Requests: 512Mi RAM, 250m CPU
- Limits: 1Gi RAM, 1000m CPU
- Replicas: 2-5 (HPA)

**Frontend (Production):**
- Requests: 64Mi RAM, 50m CPU
- Limits: 128Mi RAM, 200m CPU
- Replicas: 2-4 (HPA)

**PostgreSQL:**
- Requests: 256Mi RAM, 250m CPU
- Limits: 512Mi RAM, 500m CPU
- Replicas: 1 (StatefulSet)
- Storage: 10Gi (PVC)

### Horizontal Pod Autoscaler

```bash
# HPA Status anzeigen
kubectl get hpa -n stock-status

# Details
kubectl describe hpa backend-hpa -n stock-status

# Manuelle Skalierung (überschreibt HPA temporär)
kubectl scale deployment backend -n stock-status --replicas=3
```

### Storage

```bash
# PVC Status
kubectl get pvc -n stock-status

# PVC Details
kubectl describe pvc postgres-pvc -n stock-status

# Verwendeter Speicher
kubectl exec -n stock-status postgres-0 -- df -h /var/lib/postgresql/data
```

## Backup & Recovery

### Automatische Backups

CronJob läuft täglich um 02:00 Uhr:

```bash
# CronJob Status
kubectl get cronjob -n stock-status

# Letzte Backup-Jobs anzeigen
kubectl get jobs -n stock-status

# Backup-Logs anzeigen
kubectl logs -n stock-status -l app=postgres-backup --tail=50

# Backups im Volume prüfen
kubectl exec -n stock-status postgres-0 -- ls -lh /backups/
```

### Manuelles Backup

```bash
# Manuelles Backup erstellen
kubectl create job -n stock-status manual-backup-$(date +%Y%m%d) \
  --from=cronjob/postgres-backup

# Backup herunterladen
kubectl exec -n stock-status postgres-0 -- \
  tar czf - /backups/ | tar xzf - -C ./local-backup/
```

### Restore

```bash
# 1. Backup-Datei in Pod kopieren
kubectl cp ./backup.dump stock-status/postgres-0:/tmp/backup.dump

# 2. Restore durchführen
kubectl exec -n stock-status postgres-0 -- \
  pg_restore -U stockstatus -d stockstatus -c -v /tmp/backup.dump

# 3. Pods neustarten
kubectl rollout restart deployment/backend -n stock-status
```

## Monitoring & Health Checks

### Health Endpoints

```bash
# Backend Health (via Port-Forward)
kubectl port-forward -n stock-status svc/backend-service 8080:8080
curl http://localhost:8080/actuator/health

# Oder via Pod
kubectl exec -n stock-status $(kubectl get pod -n stock-status -l app=backend -o name | head -1) -- \
  curl -s http://localhost:8080/actuator/health | jq
```

### Logs

```bash
# Alle Logs
kubectl logs -n stock-status --all-containers=true -l app.kubernetes.io/name=stock-status --tail=100

# Backend Logs
kubectl logs -n stock-status -l app=backend -f

# Frontend Logs
kubectl logs -n stock-status -l app=frontend -f

# PostgreSQL Logs
kubectl logs -n stock-status postgres-0 -f

# Logs filtern
kubectl logs -n stock-status -l app=backend --tail=100 | grep ERROR
```

### Events

```bash
# Events im Namespace
kubectl get events -n stock-status --sort-by='.lastTimestamp'

# Pod-spezifische Events
kubectl describe pod -n stock-status <pod-name>
```

### Metrics

```bash
# Pod Metrics
kubectl top pods -n stock-status

# Node Metrics
kubectl top nodes

# Detaillierte Metrics
kubectl get --raw /apis/metrics.k8s.io/v1beta1/namespaces/stock-status/pods | jq
```

## Updates & Rollouts

### Image Update

```bash
# Image-Tag in kustomization.yaml ändern
# Oder direkt:
kubectl set image deployment/backend -n stock-status \
  backend=ghcr.io/your-username/stock-status-backend:v1.2.0

# Rollout Status
kubectl rollout status deployment/backend -n stock-status

# Rollout History
kubectl rollout history deployment/backend -n stock-status
```

### Rollback

```bash
# Zur vorherigen Version
kubectl rollout undo deployment/backend -n stock-status

# Zu spezifischer Revision
kubectl rollout undo deployment/backend -n stock-status --to-revision=2

# Status prüfen
kubectl rollout status deployment/backend -n stock-status
```

### Rolling Update

```bash
# Update durchführen
kubectl apply -k overlays/prod/

# Rollout überwachen
kubectl rollout status deployment/backend -n stock-status
kubectl rollout status deployment/frontend -n stock-status

# Pause Rollout (bei Problemen)
kubectl rollout pause deployment/backend -n stock-status

# Resume Rollout
kubectl rollout resume deployment/backend -n stock-status
```

## Troubleshooting

### Pods starten nicht

```bash
# Pod Status prüfen
kubectl get pods -n stock-status

# Pod Details und Events
kubectl describe pod -n stock-status <pod-name>

# Logs anzeigen
kubectl logs -n stock-status <pod-name>

# Vorherige Container Logs (bei CrashLoopBackOff)
kubectl logs -n stock-status <pod-name> --previous
```

### Datenbank-Verbindungsprobleme

```bash
# PostgreSQL Pod prüfen
kubectl get pod -n stock-status postgres-0

# In PostgreSQL Pod einloggen
kubectl exec -it -n stock-status postgres-0 -- psql -U stockstatus

# DNS-Auflösung testen (von Backend Pod)
kubectl exec -n stock-status <backend-pod> -- nslookup postgres-service

# Verbindung testen
kubectl exec -n stock-status <backend-pod> -- \
  nc -zv postgres-service 5432
```

### Ingress Probleme

```bash
# Ingress Status
kubectl get ingress -n stock-status

# Ingress Details
kubectl describe ingress -n stock-status stock-status-ingress

# Ingress Controller Logs
kubectl logs -n ingress-nginx -l app.kubernetes.io/name=ingress-nginx --tail=100

# TLS Zertifikat prüfen
kubectl get certificate -n stock-status
kubectl describe certificate -n stock-status stock-status-tls
```

### Performance-Probleme

```bash
# Resource Usage
kubectl top pods -n stock-status

# HPA Status
kubectl get hpa -n stock-status

# Pod Limits erreicht?
kubectl describe pod -n stock-status <pod-name> | grep -A 5 "Limits"

# Datenbank Performance
kubectl exec -n stock-status postgres-0 -- \
  psql -U stockstatus -c "SELECT * FROM pg_stat_activity;"
```

### Storage Probleme

```bash
# PVC Status
kubectl get pvc -n stock-status

# PV Details
kubectl get pv

# Speicherplatz prüfen
kubectl exec -n stock-status postgres-0 -- df -h
```

## Cleanup

### Development Environment löschen

```bash
kubectl delete -k overlays/dev/
```

### Production Environment löschen

```bash
# ACHTUNG: Löscht alle Daten!
kubectl delete -k overlays/prod/

# Namespace löschen (inkl. aller Ressourcen)
kubectl delete namespace stock-status

# PVCs bleiben bestehen! Manuell löschen:
kubectl delete pvc -n stock-status --all
```

### Einzelne Komponenten löschen

```bash
# Nur Backend
kubectl delete deployment backend -n stock-status

# Nur CronJob
kubectl delete cronjob postgres-backup -n stock-status
```

## Best Practices

### Security

1. **Secrets Management:**
   - Niemals Secrets in Git committen
   - Verwenden Sie externe Secret Stores (Vault, AWS Secrets Manager)
   - Oder sealed-secrets / SOPS

2. **Network Policies:**
   ```bash
   # Erstellen Sie NetworkPolicies um Traffic zu beschränken
   kubectl apply -f network-policies.yaml
   ```

3. **Resource Limits:**
   - Immer Requests und Limits definieren
   - Verhindert Resource Exhaustion

4. **RBAC:**
   - Minimale Berechtigungen für Service Accounts
   - Separate Namespaces für Environments

### High Availability

1. **Multiple Replicas:**
   - Backend: Mindestens 2 Replicas
   - Frontend: Mindestens 2 Replicas

2. **Pod Disruption Budgets:**
   ```yaml
   apiVersion: policy/v1
   kind: PodDisruptionBudget
   metadata:
     name: backend-pdb
   spec:
     minAvailable: 1
     selector:
       matchLabels:
         app: backend
   ```

3. **Anti-Affinity:**
   - Pods auf verschiedene Nodes verteilen
   - In Production-Overlay konfigurieren

### Monitoring

1. **Prometheus Integration:**
   - `/actuator/prometheus` Endpoint im Backend
   - ServiceMonitor für Prometheus Operator

2. **Grafana Dashboards:**
   - JVM Metrics
   - PostgreSQL Metrics
   - NGINX Metrics

3. **Alerting:**
   - Pod Restarts
   - High Memory/CPU Usage
   - Backup Failures

## Weitere Informationen

- [Kubernetes Dokumentation](https://kubernetes.io/docs/)
- [Kustomize Dokumentation](https://kustomize.io/)
- [NGINX Ingress Controller](https://kubernetes.github.io/ingress-nginx/)
- [cert-manager](https://cert-manager.io/)
- [Hauptprojekt README](../README.md)
