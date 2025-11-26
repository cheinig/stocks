# Development Kubernetes Deployment

Ein einfaches All-in-One Kubernetes Deployment für den Development-Cluster.

## Voraussetzungen

- Kubernetes Cluster (minikube, kind, k3s, etc.)
- kubectl konfiguriert
- Zugriff auf GitHub Container Registry (ghcr.io)

## GitHub Container Registry Zugriff

### Option 1: Images public machen (empfohlen für Dev)
1. Gehe zu https://github.com/users/cheinig/packages
2. Wähle dein Package (stocks-backend / stocks-frontend)
3. Package settings → Change visibility → Public

### Option 2: ImagePullSecret erstellen
Erstelle ein GitHub Personal Access Token mit `read:packages` Berechtigung:

```bash
kubectl create secret docker-registry ghcr-secret \
  --docker-server=ghcr.io \
  --docker-username=<dein-github-username> \
  --docker-password=<dein-github-token> \
  --namespace=stocks
```

## Deployment

### Alles deployen
```bash
kubectl apply -f k8s/dev-deployment.yaml
```

### Status prüfen
```bash
# Alle Pods im stocks Namespace anzeigen
kubectl get pods -n stocks

# Services anzeigen
kubectl get svc -n stocks

# Logs anschauen
kubectl logs -n stocks deployment/backend
kubectl logs -n stocks deployment/frontend
kubectl logs -n stocks deployment/postgres
```

### Zugriff auf die Anwendung

#### LoadBalancer (minikube)
```bash
minikube service frontend -n stocks
```

#### Port Forward
```bash
# Frontend
kubectl port-forward -n stocks service/frontend 8080:80

# Backend API
kubectl port-forward -n stocks service/backend 8081:8080

# PostgreSQL
kubectl port-forward -n stocks service/postgres 5432:5432
```

Dann erreichbar unter:
- Frontend: http://localhost:8080
- Backend API: http://localhost:8081
- PostgreSQL: localhost:5432

### Logs verfolgen
```bash
kubectl logs -n stocks -f deployment/backend
kubectl logs -n stocks -f deployment/frontend
```

### Neu deployen (nach Image-Update)
```bash
# Pods neu starten um neueste Images zu laden
kubectl rollout restart -n stocks deployment/backend
kubectl rollout restart -n stocks deployment/frontend
```

### Komplett löschen
```bash
kubectl delete -f k8s/dev-deployment.yaml
```

## Komponenten

Das Deployment erstellt:
- **Namespace**: `stocks`
- **PostgreSQL**: 1 Replica mit 5Gi PersistentVolume
- **Backend**: 1 Replica (Spring Boot)
- **Frontend**: 1 Replica (Angular/Nginx)

## Container Images

Die Images werden automatisch von der CI/CD Pipeline gebaut und nach ghcr.io gepusht:
- Frontend: `ghcr.io/christoph-winter/stock-status-frontend:main`
- Backend: `ghcr.io/christoph-winter/stock-status-backend:main`

## Konfiguration anpassen

Um Passwörter oder andere Konfigurationen zu ändern, editiere die entsprechenden Abschnitte in `dev-deployment.yaml`:
- **ConfigMap**: Nicht-sensitive Konfiguration
- **Secret**: Passwörter und sensitive Daten
