[![en](https://img.shields.io/badge/lang-en-red.svg)](../../../../RELEASE_NOTES.md)

# Release Notes - GreenCap K8s

## [v0.4.6] - 03/11/2025

### Removendo o uso de versões latest.

#### ✨ Atualizações:
- Removendo o uso de versões latest para todos os componentes principais (Cilium, GitLab, Harbor, Kind, Metrics Server, Prometheus, Postgres, Apache)

## [v0.4.5] - 26/10/2025

### Sistema de Instalação Personalizável.

#### ✨ Atualizações:
- **Adicionado** o parâmetro `--setup-type` ao `greencap.sh` com três modos:
    - `minimal`: instala apenas os componentes principais (kind, kubectl, cilium, ingress, apache-hello, kube-dashboard).
    - `full`: instala todos os componentes disponíveis, incluindo monitoring, harbor, gitlab, postgres e ecom-python,
    - `custom`: lê o arquivo `greencap.ini` para instalação seletiva de componentes.
- Remoção de duplicação de código e melhorias na manutenibilidade.
- Atalhos na área de trabalho para as url: Dashboard, Hello Apache e Monitoramento.

## [v0.4.4] - 2025-10-22

### Melhorias menores e atualizações de documentação.

#### ✨ Atualizações:
- **README.md**: Instruções de uso aprimoradas e detalhes para usuários em inglês e português.
- **Documentação PT-BR**: Documentação e fluxos de instalação/limpeza melhorados e alinhados com o README.

---

## [v0.4.3] - 2025-10-14

###  Novo gerenciador de instalação.

#### ✨ Novas Funcionalidades(Focado em DevEx):
- **greencap.sh**: Criado um novo gerenciador de instalação melhorando o DevEx.
- **Novo parametro --clean**: Novo parametro para limpeza dos ambientes(vagrant, aws, local) após os estudos e testes. 

---

## [v0.4.2] - 2025-10-11

###  Suporte ao GitLab.

#### ✨ Novas Funcionalidades:
- **GitLab**: GitLab é uma plataforma DevSecOps completa que ajuda as equipes a gerenciar todo o ciclo de vida do desenvolvimento de software, desde o controle de versão e colaboração de código até a automação, testes e implantação.
- **Idioma inglês no portal**: Adicionado suporte ao idioma inglês no portal https://www.greencapk8s.dev/.

---

## [v0.4.1] - 2025-09-23

### Adicionado novo parametro ao instalador.

#### ✨ Novas Funcionalidades:
- **Local Debug**: Novo parametro(--local-debug) para instalação local sem VM. Recomendado para agilizar o desenvolvimento de novas funcionalidades. 

---

## [v0.4.0] - 2025-08-XX

### ☁️ Suporte a AWS(BETA) e Melhorias de Observabilidade.

#### ✨ Novas Funcionalidades:
- **Deploy na AWS**: Suporte completo via Terraform
- **Métricas do Cluster**: Metrics Server para comandos `kubectl top`
- **Detecção de IP Público**: Sistema automático para configuração de security groups
- **Scripts Específicos por Ambiente**: Separação entre Vagrant e EC2
- **Validação de Configurações**: Verificações de pré-requisitos

---

## [v0.3.0] - 2025-07-XX

#### ✨ Funcionalidades Adicionadas:
- **Container Registry**: Harbor para gerenciamento de imagens Docker
- **Stack de Observabilidade**: Prometheus + Grafana + Jaeger

---

## [v0.2.0] - 2025-07-XX

#### ✨ Funcionalidades Adicionadas:
- **Kubernetes Dashboard**: Interface web para gerenciamento do cluster
- **Banco de Dados**: PostgreSQL instalado via Helm
- **Interface de Administração**: pgAdmin para gerenciamento do PostgreSQL
- **Aplicação Python**: API FastAPI conectando ao PostgreSQL

---

## [v0.0.1] - 2025-07-XX

### 🎉 Lançamento Inicial

**Primeira versão estável do projeto com funcionalidades básicas.**

#### ✨ Funcionalidades Adicionadas:
- **Ambiente Local com Vagrant**: Provisionamento completo de VM com Ubuntu 22.04
- **Cluster Kubernetes com Kind**: Kubernetes IN Docker para desenvolvimento local
- **Ingress Controller**: Nginx Ingress Controller configurado
- **Aplicação de Exemplo**: Hello Apache App para demonstração
- **Scripts de Automação**: Sistema modular de instalação
