# Release Notes - GreenCap K8s

## Visão Geral

Este documento contém as notas de lançamento para o projeto GreenCap K8s, um ambiente completo de estudos, desenvolvimento e testes para Kubernetes.

## [v0.4.2] - 2024-10-11

###  Suporte ao GitLab.

#### ✨ Novas Funcionalidades:
- **GitLab**: GitLab é uma plataforma DevSecOps completa que ajuda as equipes a gerenciar todo o ciclo de vida do desenvolvimento de software, desde o controle de versão e colaboração de código até a automação, testes e implantação.
- **Idioma inglês no portal**: Adicionado suporte ao idioma inglês no portal https://www.greencapk8s.dev/.

---

## [v0.4.1] - 2024-09-23

### Adicionado novo parametro ao instalador.

#### ✨ Novas Funcionalidades:
- **Local Debug**: Novo parametro(--local-debug) para instalação local sem VM. Recomendado para agilizar o desenvolvimento de novas funcionalidades. 

---

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

