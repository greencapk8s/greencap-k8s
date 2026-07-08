# ADR 0012 — Helm CLI como executor de operações Helm

## Status
Accepted

## Context
GreenCap needs to execute Helm operations (list, get, uninstall) against registered Clusters. Two alternatives were evaluated:

**Option A — `fabric8/helm-java`**: A Java Helm client from the Fabric8 ecosystem. Avoids shelling out, same tech stack. Rejected: the library is poorly maintained, lacks full parity with Helm CLI behavior, and has limited chart repository support.

**Option B — Helm CLI subprocess**: Embed the official Helm binary in the Docker runtime image and invoke it via `ProcessBuilder` for each operation. The active Cluster's decrypted Kubeconfig is written to a `Files.createTempFile` path, passed via `--kubeconfig`, and deleted in a `finally` block immediately after the call.

## Decision
Use the Helm CLI (Option B).

The Helm binary is downloaded during the Docker builder stage and copied to the runtime stage — no apt dependencies added to the runtime layer. For local development, `setup.sh` verifies and installs `helm` alongside `docker`, `kubectl`, and `minikube`.

The temporary kubeconfig file uses `600` permissions and is always deleted in `finally`, even on error, to avoid leaving decrypted credentials on disk.

## Consequences
- The Docker runtime image includes the `helm` binary (~50 MB), increasing image size.
- Helm operations depend on the `helm` binary being present and executable at runtime — its absence causes a `HelmOperationException` with a clear message.
- Kubeconfig is transiently written to disk for each operation; deleted immediately after. This is an accepted trade-off given the alternative (passing via stdin) is not supported by the Helm CLI.
- Future Helm operations (install, upgrade, rollback) follow the same pattern — no architectural change needed.
