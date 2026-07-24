# 01 — ManifestService: buscar YAML de qualquer recurso

Status: done

## O que fazer

- Criar `ManifestService` em `kubernetes/`
- Suportar os tipos: `pod`, `deployment`, `service`, `configmap`, `secret`
- Buscar o recurso via Fabric8 por namespace + name e serializar com `Serialization.asYaml()`
- Lançar `KubernetesOperationException` em falha ou recurso não encontrado

## Critério de aceite

- Compilação sem erros
- Segue padrão try-with-resources do projeto
