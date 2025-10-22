[![en](https://img.shields.io/badge/lang-en-red.svg)](../../../../infra-code-manifests/pgadmin/README.md)

# Acessando o pgAdmin no Kubernetes

Este documento explica como acessar o pgAdmin4 instalado no cluster Kubernetes via Helm e Ingress.

## Endereço de acesso

O pgAdmin está exposto via Ingress no endereço:

```
http://pgadmin.greencap:30001/
```

## Login

- **Usuário:** O e-mail definido na instalação do Helm (ex: `admin@admin.com`)
- **Senha:** A senha definida na instalação do Helm (ex: `admin-user`)

## Conectar ao banco ecom-python pelo pgadmin

Add New Server:

- **Hostname/address:** Service do postgres (ex: postgres-17)
- **Usermame:** postgres
- **Password:** Password informado na instalação (ex: user-root123)

## Referências
- [Chart Helm pgadmin4 (runix)](https://artifacthub.io/packages/helm/runix/pgadmin4)
- [Documentação oficial do pgAdmin](https://www.pgadmin.org/)
