---
id: "01"
title: "PodsView — esconder Pods Succeeded de Jobs por padrão (toggle)"
status: done
labels: [feat, frontend]
sprint: 67
---

## Contexto

Follow-up da Sprint 66 (item registrado em `docs/sprints.md`, seção "Candidatos para Próximas Sprints", "🧹 PodsView — follow-up da Sprint 66"). Com CronJobs rodando periodicamente (ex.: `node-spread-test` do `greencap-demo`, sprint 66), `PodsView` acumula muitos Pods em status `Succeeded` e a listagem fica gigante.

Decisões definidas via `/grill-with-docs`:

- Esconder por padrão **apenas** Pods com `phase == "Succeeded"` **e** `jobName` não vazio (campo `PodInfo.jobName`, já preenchido a partir do label `job-name`). Pods `Failed` permanecem sempre visíveis — são o caso de uso principal para debug.
- Novo `Checkbox` rotulado **"Hide completed Job pods"**, estado puramente local ao componente (sem persistência por usuário) — sempre inicia marcado (`true`) quando não há filtro de Job ativo.
- Quando a view abre com `?job=<jobName>` (vindo do botão "View Pods" de `JobsView`/`CronJobsView`), o checkbox inicia **desmarcado** (`false`) — evita grid vazia ao ver os pods de um Job já `Complete`. Ao limpar o filtro de Job (botão "x" do `jobFilterBanner`), o checkbox volta a iniciar marcado.
- Sem contador/label dinâmico — checkbox simples, mesmo padrão de visibilidade dos filtros de texto existentes (que também não indicam quantos itens foram filtrados).
- `CONTEXT.md` (termo `Pod`) **já atualizado** nesta sessão de planejamento com a descrição do novo comportamento — não repetir nesta issue.

## Entrega

### `PodsView.java`

1. Novo campo `private final Checkbox hideCompletedJobPodsCheckbox = new Checkbox("Hide completed Job pods", true);` (import `com.vaadin.flow.component.checkbox.Checkbox`, mesmo componente usado em `TopologiaView`/`PodLogsView`).

2. Novo helper privado:
   ```java
   private boolean isCompletedJobPod(PodInfo pod) {
       return !pod.jobName().isBlank() && "Succeeded".equals(pod.phase());
   }
   ```

3. `dataProvider.setFilter(...)` em `buildPodGrid()` — adicionar a nova condição ao predicado existente:
   ```java
   dataProvider.setFilter(item ->
       matches(item.name(), nameFilter.getValue()) &&
       matches(item.phase(), statusFilter.getValue()) &&
       matches(item.node(), nodeFilter.getValue()) &&
       (jobFilter.isBlank() || jobFilter.equals(item.jobName())) &&
       (!hideCompletedJobPodsCheckbox.getValue() || !isCompletedJobPod(item)));
   ```

4. `hideCompletedJobPodsCheckbox.addValueChangeListener(...)` — mesmo corpo dos demais listeners de filtro (`dataProvider.refreshAll()` + `UiConstants.selectFirstOrPreserve(podGrid, dataProvider, PodInfo::name)`).

5. `applyJobFilter(String jobName)` — logo após calcular `jobFilter`, definir o estado inicial do checkbox conforme a presença do filtro de Job:
   ```java
   hideCompletedJobPodsCheckbox.setValue(jobFilter.isBlank());
   ```
   Isso cobre os três casos: carregamento sem `?job=` (inicia marcado), carregamento com `?job=` (inicia desmarcado), e dismiss do `jobFilterBanner` via `applyJobFilter("")` (volta a marcado).

6. Construção/layout — novo método `buildHideCompletedJobPodsToggle()` (chamado no construtor antes de `buildPodGrid()`, já que o filtro do `dataProvider` referencia o checkbox). Adicionar o checkbox em sua própria `HorizontalLayout`, sempre visível, posicionada entre o header da seção e o `jobFilterBanner`:
   ```java
   add(UiConstants.buildSectionHeader("Pods", this::loadPods, HELP_TITLE, HELP_TEXT, podGrid, selectionActions),
           hideCompletedJobPodsRow, jobFilterBanner, noClusterMessage, clusterErrorMessage, podGrid);
   ```

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros.
- Em `samples/greencap-demo`, com o CronJob `node-spread-test` (sprint 66) já tendo gerado Pods `Succeeded`: ao abrir `PodsView` sem filtro de Job, o checkbox "Hide completed Job pods" inicia marcado e esses Pods não aparecem na grid.
- Desmarcar o checkbox exibe os Pods `Succeeded` de Jobs novamente; marcar de novo volta a escondê-los — combinado corretamente com os filtros de texto Name/Status/Node já existentes.
- Pods `Failed` pertencentes a Jobs permanecem visíveis independentemente do estado do checkbox.
- Pods sem `jobName` (não pertencem a nenhum Job) nunca são escondidos por este toggle, em qualquer estado.
- Ao navegar via "View Pods" de um Job `Complete` (`?job=<nome>`), o checkbox inicia desmarcado e os Pods `Succeeded` daquele Job aparecem normalmente; ao limpar o filtro de Job (botão "x" do banner), o checkbox volta a marcado e os Pods `Succeeded` voltam a ser escondidos.
- Sem regressão nos filtros existentes (Name, Status, Node, filtro de Job via banner) nem na seleção/`selectFirstOrPreserve`.

## Comments
