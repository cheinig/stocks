# Kubernetes Quick Start Guide

Schnellanleitung für das Deployment der Stock-Status Applikation in Kubernetes.

## Voraussetzungen Checklist

- [ ] Kubernetes Cluster läuft (minikube, kind, k3s oder Cloud)
- [ ] `kubectl` installiert und konfiguriert
- [ ] NGINX Ingress Controller installiert
- [ ] Metrics Server installiert (für HPA)
- [ ] Docker Images gebaut oder in Registry verfügbar

## 5-Minuten Setup (Development)

### 1. Cluster vorbereiten

**Mit Minikube:**
```bash
minikube start --cpus=4 --memory=8192
minikube addons enable ingress
minikube addons enable metrics-server
```

**Mit kind:**
```bash
cat <<EOF | kind create cluster --config=-
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
- role: control-plane
  kubeadmConfigPatches:
  - |
    kind: InitConfiguration
    nodeRegistration:
      kubeletExtraArgs:
        node-labels: "ingress-ready=true"
  extraPortMappings:
  - containerPort: 80
    hostPort: 80
  - containerPort: 443
    hostPort: 443
EOF

# Ingress Controller installieren
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml

# Metrics Server installieren
kubectl apply -f https://github.com/kubernetes-sigs/metrics-server/releases/latest/download/components.yaml
```

### 2. Image-Namen anpassen

Bearbeiten Sie die Dateien oder verwenden Sie Kustomize:

```bash
cd k8s

# Backend Image
sed -i 's/your-username/IHR-GITHUB-USERNAME/g' base/backend-deployment.yaml

# Frontend Image
sed -i 's/your-username/IHR-GITHUB-USERNAME/g' base/frontend-deployment.yaml

# Oder in Kustomize Overlays
sed -i 's/your-username/IHR-GITHUB-USERNAME/g' overlays/dev/kustomization.yaml
sed -i 's/your-username/IHR-GITHUB-USERNAME/g' overlays/prod/kustomization.yaml
```

### 3. Deployment durchführen

**Option A: Mit Deploy-Script (empfohlen)**
```bash
./deploy.sh dev
```

**Option B: Manuell**
```bash
# Namespace erstellen
kubectl create namespace stock-status-dev

# Secret erstellen
kubectl create secret generic stock-status-secrets \
  --namespace=stock-status-dev \
  --from-literal=POSTGRES_PASSWORD='dev123' \
  --from-literal=SPRING_DATASOURCE_PASSWORD='dev123'

# Deployment
kubectl apply -k overlays/dev/

# Status prüfen
kubectl get all -n stock-status-dev
```

### 4. Zugriff

**Port-Forwarding Setup:**
```bash
# Terminal 1: Frontend
kubectl port-forward -n stock-status-dev svc/dev-frontend-service 4200:80

# Terminal 2: Backend
kubectl port-forward -n stock-status-dev svc/dev-backend-service 8080:8080
```

**Zugriff:**
- Frontend: http://localhost:4200
- Backend API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health Check: http://localhost:8080/actuator/health

### 5. Logs anzeigen

```bash
# Alle Logs
kubectl logs -n stock-status-dev -l app=backend -f

# Spezifischer Pod
kubectl get pods -n stock-status-dev
kubectl logs -n stock-status-dev <pod-name> -f
```

## Production Setup

### 1. Voraussetzungen

```bash
# cert-manager installieren
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.13.0/cert-manager.yaml

# ClusterIssuer erstellen
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: ihre-email@example.com
    privateKeySecretRef:
      name: letsencrypt-prod
    solvers:
    - http01:
        ingress:
          class: nginx
EOF
```

### 2. Konfiguration anpassen

```bash
cd k8s/overlays/prod

# Domain anpassen
sed -i 's/stock-status.example.com/ihre-domain.com/g' patch-ingress.yaml

# ConfigMap prüfen und anpassen
nano patch-configmap.yaml
```

### 3. Secrets erstellen

```bash
# WICHTIG: Sichere Passwörter verwenden!
kubectl create namespace stock-status

kubectl create secret generic stock-status-secrets \
  --namespace=stock-status \
  --from-literal=POSTGRES_PASSWORD='SEHR-SICHERES-PASSWORT-HIER' \
  --from-literal=SPRING_DATASOURCE_PASSWORD='SEHR-SICHERES-PASSWORT-HIER'
```

### 4. Deployment

```bash
# Mit Script
./deploy.sh prod

# Oder manuell
kubectl apply -k overlays/prod/
```

### 5. DNS konfigurieren

```bash
# Ingress IP ermitteln
kubectl get ingress -n stock-status stock-status-ingress

# A-Record erstellen:
# ihre-domain.com -> <EXTERNAL-IP>
```

### 6. Verifizierung

```bash
# Alle Ressourcen prüfen
kubectl get all,pvc,ingress -n stock-status

# Health Check
curl https://ihre-domain.com/actuator/health

# Frontend
curl https://ihre-domain.com
```

## Troubleshooting

### Pods starten nicht

```bash
# Status prüfen
kubectl get pods -n stock-status-dev

# Details anzeigen
kubectl describe pod -n stock-status-dev <pod-name>

# Logs
kubectl logs -n stock-status-dev <pod-name>
```

### ImagePullBackOff

Problem: Images können nicht gepullt werden.

**Lösung:**
```bash
# Für private Registry: Image Pull Secret erstellen
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=IHR-USERNAME \
  --docker-password=IHR-TOKEN \
  --namespace=stock-status-dev

# In deployment.yaml imagePullSecrets hinzufügen
```

### CrashLoopBackOff

Problem: Container startet und crasht sofort.

**Lösung:**
```bash
# Logs des vorherigen Containers
kubectl logs -n stock-status-dev <pod-name> --previous

# Häufigste Ursachen:
# - Datenbank nicht erreichbar
# - Falsche Umgebungsvariablen
# - Secrets fehlen
```

### Pending Pods

Problem: Pods bleiben in "Pending" Status.

**Lösung:**
```bash
# Events prüfen
kubectl describe pod -n stock-status-dev <pod-name>

# Häufigste Ursachen:
# - Keine verfügbaren Nodes
# - PVC kann nicht gebunden werden
# - Resource Limits zu hoch
```

### Ingress nicht erreichbar

**Mit Minikube:**
```bash
# Tunnel starten
minikube tunnel

# Oder Ingress IP ermitteln
minikube ip

# Hosts-Datei anpassen
sudo echo "$(minikube ip) dev.stock-status.example.com" >> /etc/hosts
```

**Mit kind:**
```bash
# Port-Forwarding nutzen oder LoadBalancer MetalLB installieren
kubectl port-forward -n ingress-nginx svc/ingress-nginx-controller 8080:80
```

## Nützliche Befehle

```bash
# Alle Ressourcen im Namespace
kubectl get all -n stock-status-dev

# Events beobachten
kubectl get events -n stock-status-dev --watch

# Pods beobachten
kubectl get pods -n stock-status-dev -w

# In Pod einloggen
kubectl exec -it -n stock-status-dev <pod-name> -- /bin/sh

# Resource Usage
kubectl top pods -n stock-status-dev
kubectl top nodes

# HPA Status
kubectl get hpa -n stock-status-dev

# Backup Status
kubectl get cronjob -n stock-status-dev
kubectl get jobs -n stock-status-dev
```

## Cleanup

```bash
# Mit Script
./cleanup.sh dev

# Oder manuell
kubectl delete -k overlays/dev/
kubectl delete namespace stock-status-dev
```

## Weiterführende Dokumentation

- [Vollständige Kubernetes Dokumentation](README.md)
- [Haupt-README](../README.md)
- [Ansible Deployment](../ansible/README.md)
