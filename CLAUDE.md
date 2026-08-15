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

## Branches

Trunk único: `main` é o branch de desenvolvimento e o default do repositório. Não existem branches de longa vida — todo trabalho acontece em branch efêmero que nasce da `main`, vive dias e morre no merge. Ver `docs/adr/0023-trunk-unico-com-release-por-tag.md`.

| Branch | Vida | Papel |
|--------|------|-------|
| `main` | permanente | Trunk — protegido, só recebe merge via PR |
| `sprint-N`, `feat/*`, `fix/*` | efêmera | Trabalho em andamento; deletado após o merge |
| `release-X.Y` | sob demanda | Criado a partir de uma tag **apenas** se for preciso corrigir um release enquanto a `main` já avançou |

- PR obrigatório na `main`; o check `Docker Compose Validate / test` precisa passar
- Merge commit é o padrão (preserva os commits da entrega); squash fica disponível para PR externo com histórico ruidoso
- Um PR por sprint, não por commit

### Versionamento

A versão vem da **tag**, nunca do branch (`build.gradle.kts`):

| Situação | Versão resultante |
|----------|-------------------|
| `GREENCAP_VERSION` definida | o valor da variável (usado pelo `publish-image.yml`) |
| Tag apontando exatamente para o HEAD | a tag sem o `v` — `v0.7.10` → `0.7.10`, `v0.7.10-rc.1` → `0.7.10-rc.1` |
| Qualquer outro commit | `version.base` + `-dev` |

Fluxo de release:

1. **Release candidate (opcional)** — tagear `vX.Y.Z-rc.N` e push da tag. Publica `ghcr.io/greencapk8s/platform:X.Y.Z-rc.N` **sem mover o `:latest`**, e sai como pre-release no GitHub. Validar com `PLATFORM_IMAGE_TAG=X.Y.Z-rc.N ./setup/setup.sh`; se houver correção, commitar na `main` e tagear `-rc.N+1`
2. **Release** — tagear `vX.Y.Z` e push da tag; aí sim o `:latest` é atualizado
3. **Bump imediato** — subir `version.base` para o próximo alvo logo após a tag, senão os commits seguintes saem como `X.Y.Z-dev`, o que parece anterior ao release já publicado

## Commits
- Formato: `tipo: descrição em português`
- Tipos: `feat`, `fix`, `docs`, `refactor`, `test`, `chore`
- Exemplo: `feat: adiciona visualização de logs em tempo real`
- Co-autor: `Programado em par com: Claude IA`
- Antes de cada commit: atualizar `docs/sprints.md` refletindo o que foi entregue e incluir as alterações no mesmo commit
- A sprint inteira vive num branch efêmero (`sprint-N`) e entra na `main` por um único PR

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

As skills em `.claude/skills/` vêm do repositório [mattpocock/skills](https://github.com/mattpocock/skills), versionadas aqui como cópias e registradas em `skills-lock.json` — atualizar com `npx skills@latest update`. Exceções mantidas à mão: `greencap-run` e `greencap-stop` (próprias do projeto) e a troca de `.scratch/` por `.issue-tracker/` nas skills `code-review`, `to-tickets` e `ask-matt`, que precisa ser reaplicada após cada atualização.

### Issue tracker

Issues ficam como arquivos markdown em `.issue-tracker/`. Ver `docs/agents/issue-tracker.md`.

### Triage labels

Labels canônicos padrão do mattpocock/skills. Ver `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` na raiz + `docs/adr/`. Ver `docs/agents/domain.md`.

### Sprint archiving

`docs/sprints.md` mostra só as últimas 10 sprints em "Sprints Concluídas"; o restante vai para `docs/sprints-archive.md` e `.issue-tracker/archive/`. Ver `docs/agents/sprint-archiving.md`.

### Fluxo de Sprint

Toda sprint segue este ciclo antes de qualquer código:

0. **Branch** — criar o branch da sprint a partir da `main` atualizada: `git pull && git checkout -b sprint-N`
1. **Planejamento** — antes de iniciar, lembrar o usuário de alterar o effort para `high` (`/effort high`); usar `/grill-with-docs` para definir escopo, requisitos e restrições da sprint
2. **Issues** — criar uma issue por entrega em `.issue-tracker/sprint-N/issues/NN-slug.md` (ver `docs/agents/issue-tracker.md`)
3. **Implementação** — antes de iniciar, lembrar o usuário de alterar o effort para `medium` (`/effort medium`); codificar seguindo as convenções deste guia, marcando a issue como `in-progress`
4. **Compilação** — compilar (`./gradlew compileJava`) após cada mudança relevante; reportar erros imediatamente
5. **Aceite manual** — aguardar o usuário validar os fluxos no browser e confirmar o aceite; não avançar sem essa confirmação
6. **Testes** — para cada entrega da sprint, analisar e implementar cobertura em duas frentes: (a) **views Karibu**: guards de permissão, dialogs destrutivos (type-to-confirm, guard de seleção), validação de formulários — estender `KaribuTest`; (b) **integração**: novos métodos de serviço ou comportamentos de repositório que ainda não têm teste — estender `PostgresIntegrationTest`; compilar com `./gradlew compileTestJava` após implementar
7. **Fechamento** — somente após aceite explícito do usuário: rodar testes automatizados (`./gradlew test`), marcar a issue como `done`, atualizar `docs/sprints.md` (status ✅ e entrada na seção Sprints Concluídas), aplicar a rotina de `docs/agents/sprint-archiving.md` se a seção ultrapassar 10 entradas, e fazer commit incluindo tudo
8. **Merge** — `git push -u origin sprint-N`, abrir o PR (`gh pr create --fill`) e mesclar após a CI passar (`gh pr merge --auto --merge`); o branch da sprint é deletado no merge

> ⚠️ **NUNCA** marcar issues como `done`, mudar o status da sprint para ✅ Concluído, adicionar a sprint na seção "Sprints Concluídas" do `docs/sprints.md`, ou criar o commit de fechamento antes de receber confirmação explícita de aceite do usuário. Testes automatizados passando **não** substituem o aceite manual.
