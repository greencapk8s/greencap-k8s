# GreenCap K8s — Guia para o Claude Code

## Projeto
Plataforma web para gerenciamento de clusters Kubernetes voltada para usuários iniciantes. Monolito Spring Boot dockerizado (plug and play). Base em outras soluções como: OKD, OpenShift e Racher.

## Stack
- Java 21 + Spring Boot 3.3 + Vaadin Flow 24
- Gradle 8.8 (Kotlin DSL)
- PostgreSQL 16 + Flyway + Spring Data JPA
- Fabric8 Kubernetes Client 6.13
- Pacote raiz: `io.greencap.k8s`

## Estrutura de Pacotes
```
config/      # Beans de configuração e infraestrutura (Security, Encryption, DataInitializer)
domain/      # Entidades JPA, Repositories e Services por agregado (cluster/, user/)
kubernetes/  # Integração Fabric8 — stateless services + DTOs em dto/
ui/          # Views Vaadin e MainLayout
```

## Convenções de Código

### Geral
- Usar Lombok (`@RequiredArgsConstructor`, `@Getter`, `@Setter`, `@Slf4j`)
- Sem comentários óbvios — comentar apenas o "porquê", nunca o "o quê"
- Sem `System.out.println` — usar `log.info/debug/error` via `@Slf4j`
- Sem lógica de negócio nas Views Vaadin — apenas orquestração de UI

### Services
- `@Transactional(readOnly = true)` na classe, `@Transactional` nos métodos de escrita
- Operações Fabric8 sempre dentro de `try-with-resources` (fechar o `KubernetesClient`)
- Lançar `KubernetesOperationException` em falhas de API Kubernetes

### Views Vaadin
- Notificações sempre em `Notification.Position.BOTTOM_END`
- Badges de status via `badge` theme + variante (`success`, `error`, `contrast`)
- Formulários em `FormLayout` com `ResponsiveStep("0", 1)`

### Banco de Dados
- Sem SQL nativo — usar Spring Data JPA ou JPQL
- Migrations Flyway nomeadas: `V{n}__{descricao_snake_case}.sql`
- Nunca alterar uma migration já aplicada — criar nova migration

### Segurança
- Nunca commitar `.env`, kubeconfig, senhas ou chaves reais
- `GREENCAP_ENCRYPTION_KEY` sempre via variável de ambiente em produção
- Kubeconfig sempre encriptado antes de persistir (`EncryptionService`)

## Commits
- Formato: `tipo: descrição em português`
- Tipos: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`
- Exemplo: `feat: adiciona visualização de logs em tempo real`
- Co-autor: `Programado em par com: Claude IA`
- Antes de cada commit: atualizar `docs/sprints.md` refletindo o que foi entregue e incluir as alterações no mesmo commit

## Comentários
- Todos os comentários criados no código devem está em inglês

## Code Quality

### Nomenclatura
- Nomes revelam intenção: `findActiveClustersByUser()` não `getData()`
- Sem abreviações: `kubernetesClient` não `kc`, `namespace` não `ns`
- Booleanos como afirmações: `isActive`, `hasConnection`, `isEmpty`
- Constantes em `UPPER_SNAKE_CASE`

### Métodos
- Um método faz uma coisa — se o nome precisa de "e" (ex: `validateAndSave`), dividir em dois
- Máximo de 3 parâmetros — acima disso, criar um objeto de request/DTO
- Sem side effects escondidos: um método chamado `get*` não deve alterar estado
- Retornar `Optional` quando o resultado pode ser ausente — nunca retornar `null`

### Classes
- Responsabilidade única: uma classe tem um motivo para mudar
- Máximo ~200 linhas — acima disso, avaliar extração
- Views Vaadin: sem injeção de repositories diretamente — sempre via service

### Legibilidade
- Sem números mágicos: `int MAX_REPLICAS = 10` não `if (replicas > 10)`
- Condicionais complexas extraídas para métodos com nome descritivo
- Evitar negações duplas: `if (isActive)` não `if (!isInactive)`
- Early return para reduzir aninhamento — sem `else` após `return`

### O que evitar
- Dead code: remover código comentado, métodos não utilizados
- Duplicação: antes de criar, verificar se já existe algo equivalente no projeto
- Over-engineering: não abstrair antes da segunda ocorrência real
- Objetos anêmicos: entidades com comportamento, não apenas getters/setters

## Agent skills

### Issue tracker

Issues ficam como arquivos markdown em `.scratch/`. Ver `docs/agents/issue-tracker.md`.

### Triage labels

Labels canônicos padrão do mattpocock/skills. Ver `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` na raiz + `docs/adr/`. Ver `docs/agents/domain.md`.

### Fluxo de Sprint

Toda sprint segue este ciclo antes de qualquer código:

1. **Planejamento** — usar `/grill-with-docs` para definir escopo, requisitos e restrições da sprint
2. **Issues** — criar uma issue por entrega em `.scratch/sprint-N/issues/NN-slug.md` (ver `docs/agents/issue-tracker.md`)
3. **Implementação** — codificar seguindo as convenções deste guia, marcando a issue como `in-progress`
4. **Testes automatizados** — compilar (`./gradlew compileJava`) e rodar testes (`./gradlew test`); reportar resultado ao usuário
5. **Aceite manual** — aguardar o usuário validar os fluxos no browser e confirmar o aceite; não avançar sem essa confirmação
6. **Fechamento** — somente após aceite do usuário: marcar a issue como `done`, atualizar `docs/sprints.md` e fazer commit incluindo tudo
