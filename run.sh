#!/usr/bin/env bash
set -euo pipefail
cd "$(dirname "$0")"
MODE="${1:-docker}"
case "$MODE" in docker|k8s|build|db|stop|stop-k8s) ;; *) echo "Usage: ./run.sh [docker|k8s|build|db|stop|stop-k8s]" >&2; exit 2;; esac
need() { command -v "$1" >/dev/null || { echo "Install $1 first." >&2; exit 1; }; }
need docker
# Kubernetes commands always target the local Docker Desktop cluster, never the current context.
kube() { kubectl --context docker-desktop "$@"; }
if [[ "$MODE" == stop-k8s ]]; then
  need kubectl
  kube -n em scale deployment backend admin member --replicas=0
  kube -n em scale statefulset postgres --replicas=0
  echo "Kubernetes workloads stopped; persistent data retained."
  exit 0
fi
if ! docker info >/dev/null 2>&1; then
  if [[ "$(uname -s)" == Darwin ]] && [[ -d /Applications/Docker.app ]]; then
    echo "Starting Docker Desktop…"
    open -a Docker
    for ((i=0; i<60; i++)); do
      docker info >/dev/null 2>&1 && break
      sleep 2
    done
  fi
fi
docker info >/dev/null 2>&1 || { echo "Start Docker Desktop, then rerun ./run.sh." >&2; exit 1; }
docker compose version >/dev/null
if [[ "$MODE" == stop ]]; then
  if [[ -f .runtime/local.env ]]; then docker compose --env-file .runtime/local.env down; fi
  exit 0
fi
if [[ "$MODE" == k8s ]]; then
  need kubectl
  kube get nodes --request-timeout=10s >/dev/null 2>&1 || {
    echo "Enable Kubernetes in Docker Desktop settings and wait for it to start, then run ./run.sh k8s." >&2
    exit 1
  }
fi
umask 077
mkdir -p .runtime
if [[ ! -f .runtime/local.env ]]; then
  need openssl
  { printf 'ADMIN_USERNAME=admin\nADMIN_PASSWORD=%s\nDB_PASSWORD=%s\n' "$(openssl rand -hex 16)" "$(openssl rand -hex 24)"; } > .runtime/local.env
fi
if [[ "$MODE" == db ]]; then
  docker compose --env-file .runtime/local.env up --detach --wait postgres
  echo "PostgreSQL: localhost:5432 / database: giving / user: giving"
  echo "Password: DB_PASSWORD in .runtime/local.env"
  exit 0
fi
if [[ "$MODE" == build ]]; then
  docker compose --env-file .runtime/local.env build
  echo "Images built. Local credentials are in .runtime/local.env."
  exit 0
fi
if [[ "$MODE" == docker ]]; then
  docker compose --env-file .runtime/local.env up --build --detach --wait --wait-timeout 300
  echo "Admin: http://localhost:4200 | Members: http://localhost:4201"
  echo "Login credentials: .runtime/local.env (username: admin)"
else
  docker compose --env-file .runtime/local.env build
  kube apply -f deploy/k8s/namespace.yaml
  # Keep secrets out of command arguments and generated manifest files.
  kube -n em create secret generic em-secrets --from-env-file=.runtime/local.env --dry-run=client -o yaml | kube apply -f -
  kube apply -f deploy/k8s/
  # Local tags are rebuilt in place; restart existing workloads to use the new images.
  kube -n em rollout restart deployment/backend deployment/admin deployment/member
  kube -n em rollout status statefulset/postgres --timeout=300s
  for app in backend admin member; do kube -n em rollout status "deployment/$app" --timeout=300s; done
  echo "Admin: http://localhost:4200 | Members: http://localhost:4201"
  echo "Login credentials: .runtime/local.env (username: admin)"
  echo "Keep this terminal open. Ctrl+C stops port forwarding; workloads and data remain."
  kube -n em port-forward --address 127.0.0.1 service/admin 4200:8080 &
  ADMIN_FORWARD=$!
  kube -n em port-forward --address 127.0.0.1 service/member 4201:8080 &
  MEMBER_FORWARD=$!
  trap 'kill "$ADMIN_FORWARD" "$MEMBER_FORWARD" 2>/dev/null || true' EXIT
  trap 'exit 0' INT TERM
  while kill -0 "$ADMIN_FORWARD" 2>/dev/null && kill -0 "$MEMBER_FORWARD" 2>/dev/null; do sleep 1; done
  echo "A port forward ended. Check that ports 4200/4201 are free and rerun ./run.sh k8s." >&2
  exit 1
fi
