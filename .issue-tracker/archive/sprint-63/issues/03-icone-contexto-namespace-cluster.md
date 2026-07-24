---
id: "03"
title: "Ícone de contexto (i) nas seções do drawer — Namespace-scoped vs Cluster-scoped"
status: done
labels: [feat, frontend]
sprint: 63
depends_on: ["01"]
---

## Contexto

Para reforçar visualmente a diferença entre seções cujo conteúdo depende do Namespace ativo e seções que não dependem (são do Cluster como um todo), cada cabeçalho de seção do drawer (`MainLayout.buildNavSection`) deve exibir opcionalmente um pequeno ícone de informação "(i)" com um tooltip explicando o contexto.

Decisões de escopo via `/grill-with-docs`:

- Modelo de três níveis, mapeado às 4 seções (após a issue 01):
  - **PROJECT** e **OBSERVABILITY** → ícone com tooltip "Namespace-scoped — depends on the namespace selected above".
  - **GLOBAL** → ícone com tooltip "Cluster-scoped — independent of the selected namespace".
  - **SETTINGS** → sem ícone (é configuração de plataforma/usuário, fora do eixo namespace×cluster).
- Mecanismo: ícone `VaadinIcon.INFO_CIRCLE_O`, pequeno (~14px), cor secundária (mesma família visual do rótulo da seção, que usa `LumoUtility.FontSize.XXSMALL` + `TextColor.SECONDARY`), com tooltip via atributo HTML nativo `title` (mesmo padrão já usado no botão de Logout do `MainLayout`, linha 360: `logout.getElement().setAttribute("title", "Logout")`). Sem `Tooltip` do Vaadin, sem `Dialog`.

## Entrega

### `MainLayout.java`

- Novas constantes de classe:
  ```java
  private static final String NAMESPACE_CONTEXT_TOOLTIP =
          "Namespace-scoped — depends on the namespace selected above";
  private static final String CLUSTER_CONTEXT_TOOLTIP =
          "Cluster-scoped — independent of the selected namespace";
  ```
- `buildNavSection(String label, SideNav nav)`: extrair a construção do `Span sectionLabel` e, quando houver tooltip, envolver `sectionLabel` + um `Icon` (`VaadinIcon.INFO_CIRCLE_O.create()`, `icon.setSize("14px")`, `icon.addClassNames(LumoUtility.TextColor.SECONDARY)`, `icon.getElement().setAttribute("title", contextTooltip)`) numa `HorizontalLayout` (`setSpacing` pequeno, `setPadding(false)`, `setAlignItems(Alignment.CENTER)`), substituindo o `Span` solto no `VerticalLayout` da seção.
- Nova sobrecarga `buildNavSection(String label, SideNav nav, String contextTooltip)`; a assinatura existente `buildNavSection(String label, SideNav nav)` delega para a nova passando `null` (sem ícone — usado por SETTINGS).
- `buildDrawer()` passa a chamar:
  - `buildNavSection("PROJECT", buildVisaoGeralNav(), NAMESPACE_CONTEXT_TOOLTIP)`
  - `buildNavSection("OBSERVABILITY", buildObservabilidadeNav(), NAMESPACE_CONTEXT_TOOLTIP)`
  - `buildNavSection("GLOBAL", buildGlobalNav(), CLUSTER_CONTEXT_TOOLTIP)`
  - `buildNavSection("SETTINGS", buildConfiguracaoNav())` (sem tooltip)

## Critérios de aceite manual

- Seções **PROJECT** e **OBSERVABILITY**: pequeno ícone "i" ao lado do rótulo; ao passar o mouse, exibe "Namespace-scoped — depends on the namespace selected above".
- Seção **GLOBAL**: ícone "i" ao lado do rótulo; ao passar o mouse, exibe "Cluster-scoped — independent of the selected namespace".
- Seção **SETTINGS**: sem ícone.
- O ícone não quebra o alinhamento, tamanho (`XXSMALL`) ou cor (`SECONDARY`) do rótulo da seção, nem o comportamento do drawer redimensionável (`initResizableDrawer`).
- Comportamento visual consistente no tema claro e escuro.
