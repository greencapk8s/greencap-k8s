---
name: greencap-run
description: Start the GreenCap K8s application in the local development environment. Use when the user asks to start, run, or bring up the application.
---

# greencap-run

Starts the GreenCap K8s development environment: PostgreSQL via Docker Compose and Spring Boot via Gradle.

## Step 1 — Ensure PostgreSQL is running

```bash
docker ps --filter "name=greencap-db-dev" --filter "status=running" --format "{{.Names}}"
```

- If output is `greencap-db-dev` → already running, skip to Step 2.
- If empty → start it:

```bash
docker compose -f docker-compose.dev.yml up -d
```

Then wait until healthy:

```bash
until docker inspect greencap-db-dev --format "{{.State.Health.Status}}" 2>/dev/null | grep -q "healthy"; do sleep 2; done
```

## Step 2 — Check if Spring Boot is already running

```bash
lsof -ti:8080 > /dev/null 2>&1 && echo "running" || echo "stopped"
```

- If `running` → inform the user the app is already up on `http://localhost:8080`. Done.
- If `stopped` → continue to Step 3.

## Step 3 — Start Spring Boot

```bash
SPRING_PROFILES_ACTIVE=dev ./gradlew bootRun --console=plain > build/boot.log 2>&1 &
echo $! > build/boot.pid
```

## Step 4 — Wait for startup confirmation

```bash
until grep -q "Tomcat started\|Application run failed" build/boot.log 2>/dev/null; do sleep 3; done && grep -E "Tomcat started|Application run failed|ERROR" build/boot.log | tail -3
```

- If output contains `Tomcat started` → report success: app is running at `http://localhost:8080`.
- If output contains `Application run failed` or `ERROR` → show the last 20 lines of `build/boot.log` and report the failure.

## Notes

- Dev profile defaults: DB at `localhost:5432`, user `greencap`, password `dev123`.
- Encryption key defaults to `dev-encryption-key-change-me-32x` — no `.env` needed in dev.
- The `build/` directory is gitignored — `boot.log` and `boot.pid` are never committed.
- To stop the app use `/greencap-stop`.
