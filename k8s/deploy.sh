#!/bin/bash

# Stock-Status Kubernetes Deployment Script
# Usage: ./deploy.sh [dev|prod]

set -e

ENVIRONMENT=${1:-dev}
NAMESPACE="stock-status"
if [ "$ENVIRONMENT" == "dev" ]; then
  NAMESPACE="stock-status-dev"
fi

echo "==================================="
echo "Stock-Status Kubernetes Deployment"
echo "Environment: $ENVIRONMENT"
echo "Namespace: $NAMESPACE"
echo "==================================="
echo ""

# Farben für Output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Funktion für Success Messages
success() {
  echo -e "${GREEN}✓${NC} $1"
}

# Funktion für Error Messages
error() {
  echo -e "${RED}✗${NC} $1"
}

# Funktion für Warning Messages
warn() {
  echo -e "${YELLOW}⚠${NC} $1"
}

# Funktion für Info Messages
info() {
  echo -e "ℹ $1"
}

# Check ob kubectl installiert ist
if ! command -v kubectl &> /dev/null; then
  error "kubectl ist nicht installiert. Bitte installieren Sie kubectl."
  exit 1
fi
success "kubectl gefunden"

# Check ob Cluster erreichbar ist
if ! kubectl cluster-info &> /dev/null; then
  error "Kubernetes Cluster ist nicht erreichbar. Bitte prüfen Sie Ihre kubeconfig."
  exit 1
fi
success "Kubernetes Cluster erreichbar"

# Check ob kustomize verfügbar ist (in kubectl integriert)
if ! kubectl kustomize --help &> /dev/null; then
  error "kustomize ist nicht verfügbar. Bitte aktualisieren Sie kubectl."
  exit 1
fi
success "kustomize verfügbar"

echo ""
info "Deploying to environment: $ENVIRONMENT"
echo ""

# Preview der zu deployenden Ressourcen
info "Preview der Ressourcen:"
kubectl kustomize overlays/$ENVIRONMENT/ | grep "kind:"

echo ""
read -p "Möchten Sie fortfahren? (y/n) " -n 1 -r
echo
if [[ ! $REPLY =~ ^[Yy]$ ]]; then
  warn "Deployment abgebrochen."
  exit 0
fi

echo ""
info "Starte Deployment..."

# Secrets Check
if ! kubectl get secret stock-status-secrets -n $NAMESPACE &> /dev/null; then
  warn "Secret 'stock-status-secrets' existiert nicht im Namespace '$NAMESPACE'"
  echo ""
  echo "Bitte erstellen Sie das Secret mit:"
  echo "kubectl create secret generic stock-status-secrets \\"
  echo "  --namespace=$NAMESPACE \\"
  echo "  --from-literal=POSTGRES_PASSWORD='your-password' \\"
  echo "  --from-literal=SPRING_DATASOURCE_PASSWORD='your-password'"
  echo ""
  read -p "Secret trotzdem überspringen und fortfahren? (y/n) " -n 1 -r
  echo
  if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    exit 0
  fi
fi

# Deployment durchführen
if kubectl apply -k overlays/$ENVIRONMENT/; then
  success "Deployment erfolgreich angewendet"
else
  error "Deployment fehlgeschlagen"
  exit 1
fi

echo ""
info "Warte auf Pod-Bereitschaft..."

# Warte auf PostgreSQL
echo -n "PostgreSQL: "
if kubectl wait --for=condition=ready pod -l app=postgres -n $NAMESPACE --timeout=300s &> /dev/null; then
  success "Bereit"
else
  error "Timeout oder Fehler"
fi

# Warte auf Backend
echo -n "Backend: "
if kubectl wait --for=condition=ready pod -l app=backend -n $NAMESPACE --timeout=300s &> /dev/null; then
  success "Bereit"
else
  error "Timeout oder Fehler"
fi

# Warte auf Frontend
echo -n "Frontend: "
if kubectl wait --for=condition=ready pod -l app=frontend -n $NAMESPACE --timeout=300s &> /dev/null; then
  success "Bereit"
else
  error "Timeout oder Fehler"
fi

echo ""
success "Alle Pods sind bereit!"

echo ""
echo "==================================="
echo "Deployment-Informationen"
echo "==================================="
echo ""

# Pods anzeigen
info "Pods:"
kubectl get pods -n $NAMESPACE

echo ""

# Services anzeigen
info "Services:"
kubectl get svc -n $NAMESPACE

echo ""

# Ingress anzeigen
info "Ingress:"
kubectl get ingress -n $NAMESPACE

echo ""

# PVC anzeigen
info "PersistentVolumeClaims:"
kubectl get pvc -n $NAMESPACE

echo ""
echo "==================================="
echo "Zugriff"
echo "==================================="
echo ""

if [ "$ENVIRONMENT" == "dev" ]; then
  info "Development Environment - Port-Forwarding Setup:"
  echo ""
  echo "Backend:"
  echo "  kubectl port-forward -n $NAMESPACE svc/dev-backend-service 8080:8080"
  echo "  http://localhost:8080/actuator/health"
  echo ""
  echo "Frontend:"
  echo "  kubectl port-forward -n $NAMESPACE svc/dev-frontend-service 4200:80"
  echo "  http://localhost:4200"
  echo ""
  echo "PostgreSQL:"
  echo "  kubectl port-forward -n $NAMESPACE svc/dev-postgres-service 5432:5432"
else
  info "Production Environment:"
  INGRESS_HOST=$(kubectl get ingress -n $NAMESPACE stock-status-ingress -o jsonpath='{.spec.rules[0].host}' 2>/dev/null || echo "nicht konfiguriert")
  echo ""
  echo "Ingress Host: $INGRESS_HOST"
  echo ""
  echo "Frontend: https://$INGRESS_HOST"
  echo "Backend API: https://$INGRESS_HOST/api"
  echo "Swagger UI: https://$INGRESS_HOST/swagger-ui.html"
  echo "Health Check: https://$INGRESS_HOST/actuator/health"
fi

echo ""
echo "==================================="
echo "Nützliche Befehle"
echo "==================================="
echo ""
echo "Logs anzeigen:"
echo "  kubectl logs -n $NAMESPACE -l app=backend -f"
echo "  kubectl logs -n $NAMESPACE -l app=frontend -f"
echo "  kubectl logs -n $NAMESPACE postgres-0 -f"
echo ""
echo "Pod Status:"
echo "  kubectl get pods -n $NAMESPACE -w"
echo ""
echo "HPA Status:"
echo "  kubectl get hpa -n $NAMESPACE"
echo ""
echo "Events:"
echo "  kubectl get events -n $NAMESPACE --sort-by='.lastTimestamp'"
echo ""

success "Deployment abgeschlossen!"
