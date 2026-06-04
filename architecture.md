# GreenCap K8s — Documento de Arquitetura

> Versão 1.0 — Discovery finalizado  
> Status: Pronto para início do desenvolvimento

---

## 1. Visão Geral

Plataforma web para gerenciamento simplificado de clusters Kubernetes (OKD, OpenShift, Rancher), voltada para usuários iniciantes e médios. Entregue como uma solução **plug and play** dockerizada.

---

## 2. Decisões Técnicas

| Camada | Tecnologia | Justificativa |
|--------|-----------|---------------|
| Backend | Java 21 + Spring Boot 3.x | Expertise do time, ecossistema maduro |
| Frontend | Vaadin Flow 24 | UI 100% em Java, integração nativa Spring |
| Banco de dados | PostgreSQL 16 | Robusto, open source, suporte JSON |
| Migrations | Flyway | Controle de versão do schema |
| K8s Client | Fabric8 Kubernetes Client | Suporte nativo OKD/OpenShift + Kubernetes |
| Segurança | Spring Security | Auth local MVP, OIDC/LDAP plugável depois |
| Streaming | WebSocket (STOMP) | Logs em tempo real de pods |
| Build | Gradle (Kotlin DSL) | Moderno, flexível |
| Containerização | Docker + Docker Compose | Plug and play em qualquer ambiente |
| Dev local | Minikube + VirtualBox | Ambiente isolado e próximo da produção |

---

## 3. Arquitetura do Monolito

```
┌─────────────────────────────────────────────────────────────────┐
│                        Docker Compose                           │
│                                                                 │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │                  Spring Boot 3.x (Java 21)               │  │
│  │                                                          │  │
│  │  ┌────────────────┐  ┌─────────────┐  ┌─────────────┐  │  │
│  │  │  Vaadin Flow   │  │  REST/WS    │  │  WebSocket  │  │  │
│  │  │  (UI Layer)    │  │  (interno)  │  │  (Logs)     │  │  │
│  │  └────────────────┘  └─────────────┘  └─────────────┘  │  │
│  │           │                                              │  │
│  │  ┌────────────────────────────────────────────────────┐ │  │
│  │  │              Spring Security                       │ │  │
│  │  │   (Auth local MVP → OIDC/LDAP plugável depois)    │ │  │
│  │  └────────────────────────────────────────────────────┘ │  │
│  │           │                                              │  │
│  │  ┌────────────────────────────────────────────────────┐ │  │
│  │  │           Domain Layer (DDD light)                 │ │  │
│  │  │   cluster/ · user/ · audit/                        │ │  │
│  │  └────────────────────────────────────────────────────┘ │  │
│  │           │                                              │  │
│  │  ┌────────────────────────────────────────────────────┐ │  │
│  │  │        Fabric8 Kubernetes Client                   │ │  │
│  │  │   WorkloadService · NamespaceService               │ │  │
│  │  │   LogStreamService · DeployService                 │ │  │
│  │  └────────────────────────────────────────────────────┘ │  │
│  │           │                                              │  │
│  │  ┌────────────────────────────────────────────────────┐ │  │
│  │  │   Spring Data JPA + Flyway → PostgreSQL            │ │  │
│  │  └────────────────────────────────────────────────────┘ │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                 │
│  ┌──────────────────────┐                                       │
│  │    PostgreSQL 16      │                                      │
│  └──────────────────────┘                                       │
└─────────────────────────────────────────────────────────────────┘
                          │
                          ▼
           ┌──────────────────────────┐
           │    Kubernetes Cluster     │
           │  OKD / OpenShift /        │
           │  Rancher / Minikube       │
           └──────────────────────────┘
```

---

## 4. Estrutura de Pacotes

```
io.greencap.k8s
├── config/              # Beans de configuração Spring
├── domain/
│   ├── cluster/         # Entidade Cluster + conexão kubeconfig
│   ├── user/            # Usuários + Roles (ADMIN, OPERATOR, VIEWER)
│   └── audit/           # Log de auditoria de ações
├── kubernetes/          # Integração Fabric8 (stateless services)
└── ui/                  # Views Vaadin (MainLayout + telas)
```

---

## 5. Modelo de Dados (MVP)

### users
```sql
id, username, email, password_hash, role, active, created_at, updated_at
```

### clusters
```sql
id, name, provider (OKD|OPENSHIFT|KUBERNETES),
kubeconfig_content (text encriptado), api_url,
connection_status, created_by, created_at
```

### audit_logs
```sql
id, user_id, action, resource_type, resource_name,
namespace, cluster_id, payload (jsonb), created_at
```

---

## 6. Conexão Plug and Play ao Cluster

Dois modos suportados na UI:

### Modo 1 — Upload de arquivo kubeconfig
- Usuário faz upload do arquivo `.kube/config` via interface Vaadin
- Conteúdo é lido, validado e armazenado encriptado no PostgreSQL
- `KubernetesClientFactory` cria instância Fabric8 a partir do conteúdo

### Modo 2 — Colar conteúdo
- Campo textarea na UI para colar o YAML do kubeconfig diretamente
- Mesma pipeline de validação e persistência do Modo 1

### KubernetesClientFactory (padrão)
```java
// Cria cliente isolado por cluster — sem estado global
KubernetesClient buildClient(Cluster cluster) {
    Config config = Config.fromKubeconfig(cluster.getKubeconfigContent());
    return new KubernetesClientBuilder().withConfig(config).build();
}
```

---

## 7. Roles e Permissões (RBAC simplificado MVP)

| Role | Capacidades |
|------|------------|
| ADMIN | Tudo: usuários, clusters, deploys, delete |
| OPERATOR | Ver tudo + fazer deploys + restart pods |
| VIEWER | Somente leitura: pods, logs, namespaces |

Controle implementado via Spring Security + anotações `@Secured` nas Views Vaadin.

---

## 8. Roadmap de Sprints MVP (1-2 meses)

| Sprint | Semanas | Entregável |
|--------|---------|-----------|
| 1 | 1-2 | Setup projeto + Auth local + Login screen |
| 2 | 2-3 | Conexão cluster (kubeconfig upload/paste) + Dashboard base |
| 3 | 3-4 | Visualização de workloads (pods, deployments) + namespaces |
| 4 | 4-5 | Deploy simplificado (imagem + replicas + env vars) |
| 5 | 5-6 | Logs em tempo real (WebSocket) |
| 6 | 6-8 | RBAC simplificado + polimento + Docker image final |

---

## 9. Ambiente de Desenvolvimento

### Pré-requisitos (Linux)
```bash
# Java 21
sdk install java 21.0.3-tem   # via SDKMAN

# Minikube + VirtualBox
minikube start --driver=virtualbox --cpus=4 --memory=6g

# Kubeconfig gerado automaticamente em:
# ~/.kube/config
```

### Rodando localmente (sem Docker)
```bash
# PostgreSQL via Docker
docker run -d --name greencap-db \
  -e POSTGRES_DB=greencap \
  -e POSTGRES_USER=greencap \
  -e POSTGRES_PASSWORD=dev123 \
  -p 5432:5432 postgres:16

# Aplicação
./gradlew bootRun --args='--spring.profiles.active=dev'
```

### Rodando com Docker Compose (produção local)
```bash
cp .env.example .env
# editar .env com o kubeconfig path
docker compose up -d
```

---

## 10. Docker Compose (Plug and Play)

```yaml
services:
  greencap:
    image: greencap-k8s:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://db:5432/greencap
      - SPRING_DATASOURCE_USERNAME=${DB_USER}
      - SPRING_DATASOURCE_PASSWORD=${DB_PASSWORD}
      - GREENCAP_ENCRYPTION_KEY=${ENCRYPTION_KEY}
    depends_on:
      db:
        condition: service_healthy

  db:
    image: postgres:16
    environment:
      - POSTGRES_DB=greencap
      - POSTGRES_USER=${DB_USER}
      - POSTGRES_PASSWORD=${DB_PASSWORD}
    volumes:
      - pgdata:/var/lib/postgresql/data
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USER}"]
      interval: 10s
      retries: 5

volumes:
  pgdata:
```

---

## 11. Próximos Passos

- [ ] Criar repositório Git (`greencap-k8s`)
- [ ] Inicializar projeto com Spring Initializr + Vaadin
- [ ] Configurar `build.gradle.kts` com todas as dependências
- [ ] Criar Flyway migrations iniciais (V1, V2, V3)
- [ ] Implementar `LoginView` + `SecurityConfig`
- [ ] Testar conexão Fabric8 com Minikube
