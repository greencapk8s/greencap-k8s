# GreenCap K8s

Plataforma web para gerenciar clusters Kubernetes voltada a iniciantes: monolito Spring Boot + Vaadin Flow (Java 21), dockerizado e plug and play, inspirado em OKD, OpenShift e Rancher.

- Após cada mudança: `./gradlew compileJava` (e `./gradlew compileTestJava` nos testes). A suíte `./gradlew test` é lenta (Testcontainers) e roda uma vez, antes do commit de entrega
- Código, identificadores e comentários em inglês; comentário só explica o porquê
- Commit no formato `tipo: descrição em português`, com tipo `feat`, `fix`, `docs`, `refactor`, `test` ou `chore`
- Atribuição: a mensagem de commit termina com `Programado em par com: Claude IA` e a descrição de PR com `Programado em par com Claude.`, no lugar do rodapé padrão do Claude Code
- Todo trabalho vive num branch efêmero criado da `main` atualizada (`sprint-N` ou `<tipo>/<slug>`) e entra por PR com merge commit: `gh pr merge --auto --merge`
- `.env`, kubeconfig, senhas e chaves reais ficam fora do git

## Por tipo de tarefa

- **Código Java** — antes de escrever ou revisar: `docs/agents/code-standards.md`
- **Sprint** — feature, mudança de comportamento ou bug fix do produto, do branch ao merge: `docs/agents/sprint-flow.md`. O fechamento só começa com o aceite explícito do usuário no browser; testes verdes não são aceite
- **Release** — tag, release candidate ou bump de versão: `docs/agents/release.md`
- **Skills do mattpocock** — ao atualizá-las: `docs/agents/skills-maintenance.md`

## Agent skills

### Issue tracker

Issues ficam como arquivos markdown em `.issue-tracker/`. Ver `docs/agents/issue-tracker.md`.

### Triage labels

Labels canônicos padrão do mattpocock/skills. Ver `docs/agents/triage-labels.md`.

### Domain docs

Single-context: `CONTEXT.md` na raiz + `docs/adr/`. Ver `docs/agents/domain.md`.
