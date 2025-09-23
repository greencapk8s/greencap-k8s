# Monitoring Stack

Stack de monitoramento do cluster Kubernetes.

## Componentes

### 1. **Prometheus**
- Coletação de métricas do cluster
- Armazenamento de séries temporais

### 2. **Grafana**
- Dashboards e visualizações
- Query builder para Prometheus

## Acesso

### Grafana
- **URL**: http://grafana.local:30001
- **Usuário**: admin
- **Senha**: prom-operator

### Prometheus
- **URL**: http://prometheus.local:30001

## Referências

- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)