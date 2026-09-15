# Run EM with Docker and Kubernetes

From the repository root. Docker Desktop must be installed; `run.sh` opens it automatically on macOS if stopped. No local JDK, Maven, Node, or PostgreSQL installation is needed. First builds download dependencies and images and run backend tests.

## One command

```sh
./run.sh
```

This starts PostgreSQL, Spring Boot, and both Angular portals using Docker Compose and waits until healthy:

- Admin: http://localhost:4200
- Members: http://localhost:4201
- Username: `admin`. Find the generated password in `.runtime/local.env`.

```sh
cat .runtime/local.env
```

The credentials file is private (owner permissions only), ignored by Git, and excluded from Docker builds. Do not share it. Preserve it across restarts: changing it does not change existing database or administrator passwords.

To stop containers while preserving the database:

```sh
./run.sh stop
```

## Kubernetes, one command

Enable Kubernetes under Docker Desktop settings and wait for it to become ready. Ensure ports 4200 and 4201 are free; stop the Compose stack first if running.

```sh
./run.sh k8s
```

This builds the images, creates the `em` namespace and Secret, deploys PostgreSQL and all three application services, waits for readiness, and forwards the two UI ports. Keep the terminal open. Ctrl+C ends forwarding, but the workloads remain running.

This local setup specifically targets the `docker-desktop` context. It uses Docker Desktop’s containerd registry mirror with `imagePullPolicy: Always`, so rebuilt local image tags are refreshed, and the default storage class for PostgreSQL. Enable Docker Desktop’s containerd image store. Clusters without this local mirror need the images pushed to a registry and the manifest image references updated first. There is no automatic deployment to an arbitrary current context.

## Kubernetes, individual kubectl commands

Prepare Docker images and credentials without starting Compose:

```sh
./run.sh build
```

Verify the local cluster:

```sh
kubectl --context docker-desktop get nodes
kubectl --context docker-desktop get storageclass
```

Create the namespace and credentials Secret:

```sh
kubectl --context docker-desktop apply -f deploy/k8s/namespace.yaml
kubectl --context docker-desktop -n em create secret generic em-secrets \
  --from-env-file=.runtime/local.env --dry-run=client -o yaml | \
  kubectl --context docker-desktop apply -f -
```

Deploy all services and wait for startup:

```sh
kubectl --context docker-desktop apply -f deploy/k8s/
kubectl --context docker-desktop -n em rollout status statefulset/postgres --timeout=300s
kubectl --context docker-desktop -n em rollout status deployment/backend --timeout=300s
kubectl --context docker-desktop -n em rollout status deployment/admin --timeout=300s
kubectl --context docker-desktop -n em rollout status deployment/member --timeout=300s
```

Forward the admin portal in one terminal:

```sh
kubectl --context docker-desktop -n em port-forward --address 127.0.0.1 service/admin 4200:8080
```

Forward the member portal in a second terminal:

```sh
kubectl --context docker-desktop -n em port-forward --address 127.0.0.1 service/member 4201:8080
```

Open http://localhost:4200 and http://localhost:4201. Each Nginx UI forwards `/api` to Spring Boot, so session login and CSRF protection use the same origin. In Kubernetes, the database and backend do not expose host ports. Compose binds PostgreSQL only to 127.0.0.1:5432. Browser cookies are shared across localhost ports; use separate browser profiles if testing admin and member accounts simultaneously.

Inspect workloads, storage, events, and logs:

```sh
kubectl --context docker-desktop -n em get pods,services,pvc
kubectl --context docker-desktop -n em get events --sort-by=.lastTimestamp
kubectl --context docker-desktop -n em logs deployment/backend --tail=100 -f
kubectl --context docker-desktop -n em logs statefulset/postgres --tail=100
kubectl --context docker-desktop -n em describe deployment backend
```

Rebuild and apply a code change (restarts are necessary because the local image tag stays the same):

```sh
./run.sh build
kubectl --context docker-desktop apply -f deploy/k8s/
kubectl --context docker-desktop -n em rollout restart deployment/backend deployment/admin deployment/member
kubectl --context docker-desktop -n em rollout status deployment/backend --timeout=300s
```

Stop workloads without deleting the PostgreSQL PVC:

```sh
kubectl --context docker-desktop -n em scale deployment backend admin member --replicas=0
kubectl --context docker-desktop -n em scale statefulset postgres --replicas=0
```

Run `./run.sh k8s` again to resume. Compose volumes and Kubernetes PVCs are separate databases: records do not migrate between the two modes.

## Architecture and deployment limits

Docker builds use JDK 21/Maven and Node 24 in build stages, with a JRE and unprivileged Nginx in the runtime images. The Spring Boot backend exposes health probes only; readiness includes database availability. Other actuator endpoints remain unexposed. PostgreSQL data uses a named Compose volume or Kubernetes PVC. The local backend uses one replica because sessions are in memory; configure shared sessions before scaling replicas.

The manifests are for local development, without public ingress or TLS. Production needs a registry, immutable image versions, HTTPS, appropriate secure-cookie settings, managed secrets, database backups/migrations, and resource sizing. Deleting the namespace or PVC can destroy Kubernetes data. `./run.sh stop` and `./run.sh stop-k8s` retain data.

References: [Kubernetes health probes](https://kubernetes.io/docs/tasks/configure-pod-container/configure-liveness-readiness-startup-probes/), [Docker Compose startup order](https://docs.docker.com/compose/how-tos/startup-order/).

## Local PostgreSQL and tables

PostgreSQL 17 runs locally in Docker. Start just the database with:

```sh
./run.sh db
```

Connection details for a desktop SQL client:

- Host: `localhost`
- Port: `5432`
- Database: `giving`
- Username: `giving`
- Password: `DB_PASSWORD` from `.runtime/local.env`

Inspect tables using the bundled `psql` client (no host installation required):

```sh
docker compose --env-file .runtime/local.env exec postgres psql -U giving -d giving
```

Inside `psql`:

```sql
\dt
SELECT id, name, mobile_no, active FROM member;
SELECT id, member_id, contribution_month, amount FROM donation;
SELECT id, name, start_date, end_date FROM election;
\q
```

The backend creates the tables on first startup. Table names are `master`, `member`, `pastor`, `app_account`, `donation`, `donation_denomination`, `election`, `candidate`, and `vote`. Credentials are hashed in `app_account`.

For Kubernetes, inspect the database directly:

```sh
kubectl --context docker-desktop -n em exec -it postgres-0 -- psql -U giving -d giving
```

Or forward PostgreSQL to a SQL client (stop the Compose database first if port 5432 is in use):

```sh
kubectl --context docker-desktop -n em port-forward service/postgres 5432:5432
```

Back up local Compose data:

```sh
mkdir -p .runtime/backups
docker compose --env-file .runtime/local.env exec -T postgres pg_dump -U giving -d giving > .runtime/backups/giving.sql
```

The `em_postgres-data` Docker volume retains local data when containers stop or are recreated. Do not delete this volume or use `docker compose down -v` unless you intend to erase the data.

## Verification performed

Both Angular Docker images built successfully. The JDK 21 backend image passed all five workflow tests. All four Compose services became healthy, and all four Kubernetes pods became Ready. Session login, CSRF, contribution and election endpoints passed through Docker portals and the Kubernetes admin port forward. All seven tables were verified in each PostgreSQL database. Kubernetes test workloads were scaled to zero with their PVC retained; the Docker stack remains the active local environment.
