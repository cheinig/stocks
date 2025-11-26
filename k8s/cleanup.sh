#!/bin/bash

# Stock-Status Kubernetes Cleanup Script
# Usage: ./cleanup.sh [dev|prod]

set -e

ENVIRONMENT=${1:-dev}
NAMESPACE="stock-status"
if [ "$ENVIRONMENT" == "dev" ]; then
  NAMESPACE="stock-status-dev"
fi

# Farben für Output
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

warn() {
  echo -e "${YELLOW}⚠${NC} $1"
}

error() {
  echo -e "${RED}✗${NC} $1"
}

echo "==================================="
echo "Stock-Status Kubernetes Cleanup"
echo "Environment: $ENVIRONMENT"
echo "Namespace: $NAMESPACE"
echo "==================================="
echo ""

warn "ACHTUNG: Dies wird alle Ressourcen im Namespace '$NAMESPACE' löschen!"
warn "Alle Daten gehen verloren, inklusive der Datenbank!"
echo ""
read -p "Sind Sie sicher? Geben Sie 'yes' ein um fortzufahren: " -r
echo
if [[ ! $REPLY == "yes" ]]; then
  echo "Cleanup abgebrochen."
  exit 0
fi

echo ""
echo "Lösche Ressourcen..."

# Delete via kustomize
if kubectl delete -k overlays/$ENVIRONMENT/; then
  echo "✓ Ressourcen gelöscht"
else
  error "Fehler beim Löschen der Ressourcen"
fi

echo ""
read -p "Möchten Sie auch die PersistentVolumeClaims löschen? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  kubectl delete pvc -n $NAMESPACE --all
  echo "✓ PVCs gelöscht"
fi

echo ""
read -p "Möchten Sie den Namespace '$NAMESPACE' löschen? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
  kubectl delete namespace $NAMESPACE
  echo "✓ Namespace gelöscht"
fi

echo ""
echo "✓ Cleanup abgeschlossen!"
