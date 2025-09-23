# Acessando o pgAdmin no Kubernetes

Este documento explica como acessar o pgAdmin4 instalado no cluster Kubernetes via Helm e Ingress.

## 1. Endereço de acesso

O pgAdmin está exposto via Ingress no endereço:

```
http://pgadmin.local:30001/
```

## 2. Login

- **Usuário:** O e-mail definido na instalação do Helm (ex: `admin@admin.com`)
- **Senha:** A senha definida na instalação do Helm (ex: `admin-user`)

## 3. Referências
- [Chart Helm pgadmin4 (runix)](https://artifacthub.io/packages/helm/runix/pgadmin4)
- [Documentação oficial do pgAdmin](https://www.pgadmin.org/)
