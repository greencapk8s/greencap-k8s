[![en](https://img.shields.io/badge/lang-en-red.svg)](../../../../helm-values/harbor/README.md)

# Harbor - Container Registry

Este documento explica como acessar o Harbor pela primeira vez após a instalação via Helm.

## Endereço de acesso

O Harbor está exposto via Ingress no endereço:

```
https://core.harbor.greencap:30002
```

## Credenciais padrão

- **Usuário:** `admin`
- **Senha:** `Harbor12345`

> **Observação:** Se você personalizou a senha durante a instalação do Helm, use a senha definida no arquivo de valores.

## Primeiro acesso

1. Abra o navegador e acesse: `https://core.harbor.greencap:30002`
2. Faça login com as credenciais acima
3. Na primeira vez, o Harbor pode solicitar que você altere a senha do usuário `admin`

## 5. Operações Docker

### Exemplos Simples

#### Login no Harbor
```bash
docker login core.harbor.greencap:30001
# Usuário: admin
# Senha: Harbor12345
```

#### Fazer push de uma imagem para o Harbor
```bash
# Fazer tag da sua imagem
docker tag hello-world:latest core.harbor.greencap:30001/library/hello-world:latest

# Fazer push para o Harbor
docker push core.harbor.greencap:30001/library/hello-world:latest
```

#### Fazer pull de uma imagem do Harbor
```bash
# Fazer pull do Harbor
docker pull core.harbor.greencap:30001/library/hello-world:latest
```

## Referências

- [Documentação oficial do Harbor](https://goharbor.io/docs/)
- [Helm Chart Harbor](https://artifacthub.io/packages/helm/harbor/harbor) 