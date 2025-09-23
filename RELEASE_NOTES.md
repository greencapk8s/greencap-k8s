# Release Notes - K8S Playground

## Visão Geral

Este documento contém as notas de lançamento para o projeto K8S Playground, um ambiente completo de estudos, desenvolvimento e testes para Kubernetes.


## [v0.4.0] - 2024-08-XX

### ☁️ Suporte a AWS(BETA) e Melhorias de Observabilidade.

#### ✨ Novas Funcionalidades:
- **Deploy na AWS**: Suporte completo via Terraform
- **Métricas do Cluster**: Metrics Server para comandos `kubectl top`
- **Detecção de IP Público**: Sistema automático para configuração de security groups
- **Scripts Específicos por Ambiente**: Separação entre Vagrant e EC2
- **Validação de Configurações**: Verificações de pré-requisitos

---

## [v0.3.0] - 2024-07-XX

#### ✨ Funcionalidades Adicionadas:
- **Container Registry**: Harbor para gerenciamento de imagens Docker
- **Stack de Observabilidade**: Prometheus + Grafana + Jaeger

---

## [v0.2.0] - 2024-07-XX

#### ✨ Funcionalidades Adicionadas:
- **Kubernetes Dashboard**: Interface web para gerenciamento do cluster
- **Banco de Dados**: PostgreSQL instalado via Helm
- **Interface de Administração**: pgAdmin para gerenciamento do PostgreSQL
- **Aplicação Python**: API FastAPI conectando ao PostgreSQL

---

## [v0.0.1] - 2024-07-XX

### 🎉 Lançamento Inicial

**Primeira versão estável do projeto com funcionalidades básicas.**

#### ✨ Funcionalidades Adicionadas:
- **Ambiente Local com Vagrant**: Provisionamento completo de VM com Ubuntu 22.04
- **Cluster Kubernetes com Kind**: Kubernetes IN Docker para desenvolvimento local
- **Ingress Controller**: Nginx Ingress Controller configurado
- **Aplicação de Exemplo**: Hello Apache App para demonstração
- **Scripts de Automação**: Sistema modular de instalação

