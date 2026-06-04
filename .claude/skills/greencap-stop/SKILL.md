---
name: greencap-stop
description: Stop the GreenCap K8s application in the local development environment. Use when the user asks to stop, shut down, or bring down the application.
---

# greencap-stop

Stops the GreenCap K8s Spring Boot process. The PostgreSQL container is intentionally left running to preserve data between sessions — stop it explicitly only if the user asks.

## Step 1 — Kill the JVM on port 8080

Gradle spawns a child JVM that outlives the Gradle wrapper process. Killing by port is the only reliable way to reach it.

```bash
lsof -ti:8080 | xargs -r kill && echo "stopped" || echo "nothing on port 8080"
```

- If `stopped` → JVM terminated. Clean up PID file:

```bash
rm -f build/boot.pid
```

- If `nothing on port 8080` → app was already down. Clean up anyway:

```bash
rm -f build/boot.pid
```

## Step 2 — Kill any lingering Gradle wrapper process

```bash
pkill -f "[g]radlew bootRun" 2>/dev/null && echo "gradle wrapper killed" || echo "no wrapper found"
```

## Step 3 — Confirm port 8080 is free

```bash
lsof -ti:8080 && echo "still in use" || echo "clean"
```

- If `clean` → done. Report to user that the app is stopped.
- If `still in use` → force kill:

```bash
lsof -ti:8080 | xargs -r kill -9
```

## Step 4 (optional) — Stop PostgreSQL

Only if the user explicitly asks to stop the database too:

```bash
docker stop greencap-db-dev
```

## Notes

- PostgreSQL is left running by default to avoid data loss and slow restarts.
- `build/boot.pid` is removed on clean stop. If it lingers after an unexpected crash, delete it manually before running `/greencap-run` again.
- **For a restart:** run `/greencap-stop` followed by `/greencap-run`. The database stays up, so the restart is fast — only the JVM restarts.
- To start the app again use `/greencap-run`.
