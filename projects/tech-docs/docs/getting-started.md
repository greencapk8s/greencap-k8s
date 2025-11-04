# Getting Started with GreenCap K8s

Welcome to the GreenCap platform, this guide will help you get up and running quickly.

## Accessing Services

Services are available at:

| Service | URL | Credentials |
|---------|-----|-------------|
| Dashboard | https://kubernetes-dashboard.greencap:30002 | - |
| Grafana | http://grafana.greencap:30001 | admin / prom-operator |
| Prometheus | http://prometheus.greencap:30001 | - |
| Harbor | http://harbor.greencap:30002 | admin / Harbor12345 |
| Tech Docs | http://tech-docs.greencap:30001 | - |

!!! warning "Hostnames"
    Make sure the hostnames are added to your `/etc/hosts` file. The installation scripts should handle this automatically.

## Troubleshooting

### Verify Installation

Check that all pods are running:

```bash
kubectl get pods --all-namespaces
```

### Pods Not Starting

Check pod status and logs:

```bash
kubectl get pods -n <namespace>
kubectl describe pod <pod-name> -n <namespace>
kubectl logs <pod-name> -n <namespace>
```

### Ingress Not Working

Verify ingress controller is running:

```bash
kubectl get pods -n ingress-nginx
kubectl get ingressclass
```

### Services Unreachable

Check if hostnames are in `/etc/hosts`:

```bash
cat /etc/hosts | grep greencap
```

---

!!! question "Need Help?"
    Check the component-specific documentation or reach out to the GreenCap team.

