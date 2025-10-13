# Harbor - Container Registry

Este documento explica como acessar o Harbor pela primeira vez após a instalação via Helm.

## 1. Endereço de acesso

O Harbor está exposto via Ingress no endereço:

```
https://core.harbor.domain:30002
```

## 2. Credenciais padrão

- **Usuário:** `admin`
- **Senha:** `Harbor12345`

> **Observação:** Se você personalizou a senha durante a instalação do Helm, use a senha definida no arquivo de valores.

## 3. Primeiro acesso

1. Abra o navegador e acesse: `https://core.harbor.domain:30002`
2. Faça login com as credenciais acima
3. Na primeira vez, o Harbor pode solicitar que você altere a senha do usuário `admin`

## 4. Configuração inicial

Após o primeiro login:

1. **Crie um projeto:** Vá em "Projects" → "New Project"
2. **Configure o projeto:** Defina nome, visibilidade (public/private) e descrição
3. **Configure usuários (opcional):** Vá em "Administration" → "Users" para adicionar novos usuários

## 5. Operações Docker

### Exemplos Simples

#### Login no Harbor
```bash
docker login core.harbor.domain:30001
# Usuário: admin
# Senha: Harbor12345
```

#### Fazer push de uma imagem para o Harbor
```bash
# Fazer tag da sua imagem
docker tag hello-world:latest core.harbor.domain:30001/library/hello-world:latest

# Fazer push para o Harbor
docker push core.harbor.domain:30001/library/hello-world:latest
```

#### Fazer pull de uma imagem do Harbor
```bash
# Fazer pull do Harbor
docker pull core.harbor.domain:30001/library/hello-world:latest
```

## 6. Referências

- [Documentação oficial do Harbor](https://goharbor.io/docs/)
- [Helm Chart Harbor](https://artifacthub.io/packages/helm/harbor/harbor) 