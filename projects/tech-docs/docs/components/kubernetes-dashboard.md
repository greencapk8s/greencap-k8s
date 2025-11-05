# Kubernetes Dashboard

The Kubernetes Dashboard is a web-based user interface for Kubernetes clusters, providing an easy way to visualize and manage cluster resources.

## Overview

The Kubernetes Dashboard offers:

- 🖥️ **Visual Interface**: User-friendly web UI for cluster management
- 📊 **Resource Monitoring**: View pods, deployments, services, and more
- 🔍 **Log Viewer**: Access container logs directly from the UI
- 📈 **Resource Metrics**: CPU and memory usage visualization
- ⚙️ **Management**: Create, edit, and delete resources
- 🔐 **RBAC Integration**: Secure access control

## Access

### Login Steps

1. Open [https://kubernetes-dashboard.greencap:30002](https://kubernetes-dashboard.greencap:30002) in your browser
2. Select **Token** authentication method
3. Paste the token(read step Authentication)
4. Click **Sign in**

### Authentication

The dashboard requires a token for authentication. Get your access token:

```bash
# Retrieve from the secret
kubectl describe secrets admin-user -n kubernetes-dashboard
```

!!! warning "Token Expiration"
    Bearer tokens created with `kubectl create token` expire after 1 hour by default. If your token expires, generate a new one using the command below.

### Generate a new token

```bash
# Generate a new token
kubectl -n kubernetes-dashboard create token admin-user
```

---

!!! tip "Pro Tip"
    Bookmark the dashboard URL and save a token in a password manager for quick access. For security, use tokens with short expiration times and regenerate them regularly.

