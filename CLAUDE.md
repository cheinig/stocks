# Stocks — Projektkontext für Claude

Spring-Boot-Backend + Angular-Frontend + PostgreSQL. Läuft im k3s-Cluster
(Namespace `stocks`), Frontend extern über LoadBalancer **10.0.9.204**.

## WICHTIG: Deployment läuft über GitOps — nicht von hier aus deployen

Dieses Projekt wird **nicht** mehr aus diesem Repo deployed. Die Auslieferung
ist auf **ArgoCD / GitOps** umgestellt und lebt im separaten Repo
**`homelab-infra`** (`~/projects/homelab-infra`):

- Laufende Manifeste: `homelab-infra/manifests/stocks/` (Deployments, Services,
  ConfigMap, PVC, SealedSecrets)
- ArgoCD-App: `homelab-infra/apps/stocks.yaml` (Auto-Sync **mit `selfHeal=true`**)

**Konsequenz — nicht tun:**
- KEIN `kubectl apply/edit/delete`, kein `ansible deploy`, kein `kubectl rollout`
  gegen den Namespace `stocks`. ArgoCD `selfHeal` rollt solche Änderungen
  automatisch zurück.
- Die Verzeichnisse `k8s/` (Kustomize) und `ansible/` in diesem Repo sind
  **abgelöst / Altlast** — nicht mehr zum Deployen verwenden (sie wichen ohnehin
  vom Live-Zustand ab: dort StatefulSet, live Deployment).

**Laufzeit ändern = im `homelab-infra`-Repo ändern + pushen.** ArgoCD synct dann.

## Eine neue Version ausrollen

1. Code ändern, auf `main` pushen → **CI (GitHub Actions) baut & pusht** die
   Images nach GHCR: `ghcr.io/cheinig/stocks-backend` und `-frontend`, Tags
   `latest`, `main-<sha>`, semver (bei `v*`-Tag).
2. **Deploy = unveränderliche Image-Referenz in
   `homelab-infra/manifests/stocks/<backend|frontend>-deployment.yaml` bumpen
   + pushen.** ArgoCD rollt aus.

Die Deployments sind bewusst auf **Digest** gepinnt
(`...@sha256:...`), nicht auf `:latest` — so ist reproduzierbar, was läuft, und
ein Release ist ein expliziter, nachvollziehbarer Bump. Für die neue Version den
Digest (oder einen unveränderlichen Tag wie `main-<sha>`/semver) des frischen
CI-Builds eintragen. `:latest` NICHT wieder verwenden (ArgoCD würde kein
Redeploy auslösen).

## Secrets

Secrets sind **SealedSecrets** in `homelab-infra/manifests/stocks/`
(`sealedsecret-stock-status.yaml`, `sealedsecret-ghcr.yaml`). Klartext-Secrets
gehören **nie** ins Git.

Neues/ändern eines Secrets (kubeseal-CLI ist installiert, Controller in
`kube-system`):
```bash
kubectl create secret generic <name> -n stocks --dry-run=client \
  --from-literal=KEY=VALUE -o yaml \
  | kubeseal --controller-namespace kube-system \
      --controller-name sealed-secrets-controller --format yaml \
  > ~/projects/homelab-infra/manifests/stocks/sealedsecret-<name>.yaml
```
Ein **bereits existierendes** Cluster-Secret adoptieren: vorher
`kubectl -n stocks annotate secret <name> sealedsecrets.bitnami.com/managed=true`
setzen, sonst weigert sich der Controller.

## Datenbank / Daten

- PostgreSQL läuft in-cluster (`deploy/postgres`), Daten auf PVC `postgres-pvc`
  (5Gi). Die PVC ist in `homelab-infra` mit `Prune=false` geschützt — **nie**
  löschen.
- Backup (logisch):
  ```bash
  kubectl -n stocks exec deploy/postgres -- sh -c \
    'PGPASSWORD="$POSTGRES_PASSWORD" pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" --no-owner' \
    > ~/stocks-db-backup-$(date +%F).sql
  ```
- DB: `stockstatus`, User `postgres`. Backend nutzt Flyway-Migrationen
  (`SPRING_JPA_HIBERNATE_DDL_AUTO=validate`) — Schemaänderungen über Flyway, nicht
  per Hand.

## Kurzreferenz

| | |
|---|---|
| Namespace | `stocks` |
| Frontend extern | http://10.0.9.204 |
| Images | `ghcr.io/cheinig/stocks-{backend,frontend}` |
| GitOps-Quelle | `~/projects/homelab-infra` (`apps/stocks.yaml`, `manifests/stocks/`) |
| ArgoCD | https://10.0.9.205 |
