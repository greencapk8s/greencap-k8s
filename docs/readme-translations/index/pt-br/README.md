# GreenCap K8s

## Descrição

GreenCap é um projeto que fornece um ambiente completo de estudos, desenvolvimento e testes para Kubernetes:

Ele é ideal para desenvolvedores que precisam de um playground completo para testar aplicações Kubernetes incluindo: registry de containers, banco de dados, monitoramento, logs, ci/cd(gitlab) e muito mais.

Algumas ferramentas que compõe a plataforma:

- **Kind**: Kubernetes in Docker
- **Ingress**: Nginx
- **Container Registry**: Harbor para gerenciamento de imagens Docker
- **Banco de Dados**: PostgreSQL com interface pgAdmin
- **Aplicação de Exemplo**: API FastAPI em Python conectando ao PostgreSQL
- **Dashboard**: Kubernetes Dashboard para monitoramento
- **Aplicação Web**: Hello Apache App para demonstração
- **Stack de Observabilidade**: Prometheus + Grafana + Jaeger para monitoramento completo
- **Git**: GitLab
- **CI/CD**: GitLab

## Pré-requisitos

- [Vagrant](https://www.vagrantup.com/)
- [VirtualBox](https://www.virtualbox.org/) (ou outro provider compatível com Vagrant)

## Como usar

1. **Clone o repositório:**
   ```sh
   git clone git@github.com:greencapk8s/greencap-k8s.git
   cd greencap-k8s
   ```

2. **Suba o ambiente:**

   - **Local com Vagrant:**
     ```sh
     # Com GUI
     ./greencap.sh --vagrant --gui --memory 8192 --cpus 4
     
     # Sem GUI
     ./greencap.sh --vagrant --no-gui --memory 4096 --cpus 2
     ```

     Acesso a máquina virtual via ssh:
     ```sh
     vagrant ssh
     ```
   
   - **AWS EC2 (via Terraform):**
     
     Por padrão é executado o terraform plan:
     
     ```sh
     ./greencap.sh --aws --instance-type t3a.xlarge --region <region> --key-name <ec2-key-pair> --public-ip <your-public-ip> --ami-id <ubuntu-ami>
     ```

     Para aplicar, adicionar o parametro(`--auto-approve`) no final do comando:

     ```sh
     ./greencap.sh --aws --instance-type t3a.xlarge --region <region> --key-name <ec2-key-pair> --public-ip <your-public-ip> --ami-id <ubuntu-ami> --auto-approve
     ```

## Validação de Funcionamento

- **Com interface gráfica (GUI):**
  1. Acessar a máquina virtual via VirtualBox.
     - Usuário padrão da VM: **vagrant**
     - Senha padrão da VM: **vagrant**
  2. **Hello Apache App**: Acesse http://domain.local:30001/hello-apache/
     - Você deve ver a página de boas-vindas do Hello Apache App
     - ![Exemplo Hello Apache App](./images/hello-apache-app.png)
  3. **Kubernetes Dashboard**: Acesse https://kubernetes-dashboard.greencap:30002/
     - Token de acesso: `/home/vagrant/greencap/dash-token` na VM
     - ![Kubernetes Dashboard](./images/kube-dashboard.png)

- **Somente terminal (sem GUI):**
  1. Acesse a VM com `vagrant ssh`
  2. **Teste Hello Apache App**:
     ```sh
     curl -v http://domain.local:30001/hello-apache/
     ```
     
## Limpeza do Ambiente

Para remover/limpar completamente o ambiente criado (máquina virtual, arquivos, imagens), utilize o parâmetro `--clean`:

#### **Ambiente Vagrant**

```sh
./greencap.sh --clean --vagrant
```

Esse comando irá destruir a VM.

#### **Ambiente AWS (Terraform/EC2)**

```sh
./greencap.sh --clean --aws
```

Esse comando irá executar o Terraform destroy e remover recursos provisionados na AWS (instâncias, discos, etc).

#### **Ambiente Local (sem Vagrant/AWS)**
Se você realizou a instalação diretamente em sua máquina local (fora do Vagrant ou AWS), limpe com:

```sh
./greencap.sh --clean --local-debug
```

Esse comando irá deletar o cluster criado com o Kind.

## Referências

- [Kind - Kubernetes IN Docker](https://kind.sigs.k8s.io/)
- [Ingress Nginx Controller](https://kubernetes.github.io/ingress-nginx/)
- [Vagrant](https://www.vagrantup.com/)
- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com/)
- [Jaeger](https://www.jaegertracing.io/)
- [Postgres](https://www.postgresql.org/docs/)
- [pgAdmin](https://www.pgadmin.org/docs/)
- [Gitlab](https://docs.gitlab.com/)
- [mascosta](https://github.com/mascosta/docs/blob/main/kind-ingress-nginx/README.md)
