# 02 — Developer Experience como primeira seção do menu + New Application dentro dela

Status: done

Hoje a ordem do drawer (`MainLayout.buildDrawer`) é: logo, um item avulso "New Application" sem seção (`buildNewApplicationNav()`, renderizado direto abaixo do logo, sem label nem tooltip de contexto), PROJECT, GLOBAL, DEVELOPER EXPERIENCE (hoje só com "Templates Catalog" visível — Operators existe no código mas fica oculto por `OPERATORS_MENU_VISIBLE = false`), SETTINGS.

Esta entrega reordena o menu para: logo, **DEVELOPER EXPERIENCE** (agora a primeira seção), PROJECT, GLOBAL, SETTINGS — sem alterar conteúdo ou ordem interna de PROJECT, GLOBAL e SETTINGS.

"New Application" deixa de ser um item avulso e passa a ser um item da seção DEVELOPER EXPERIENCE, junto de "Templates Catalog". Ordem interna da seção: **Templates Catalog acima de New Application** (Operators, se algum dia for reativado via a flag, mantém sua posição atual acima de Templates Catalog). "New Application" herda o tooltip de contexto da seção ("Cluster-scoped — independente do namespace selecionado no header") — coerente com o comportamento real da view: `DeployApplicationView` tem seu próprio campo "Target namespace" e cria o Namespace do zero, não depende do namespace ativo no header.

O botão "New Application" mantém o mesmo ícone (`VaadinIcon.PLUS_CIRCLE`), a mesma view de destino (`DeployApplicationView`) e o mesmo comportamento de dimming quando o cluster está inacessível (via `clusterDependentNavItems`) — só muda de seção e posição, sem mudança de comportamento.

Fora de escopo: qualquer alteração em PROJECT, GLOBAL, SETTINGS ou na flag `OPERATORS_MENU_VISIBLE`.

Cobertura de teste (Karibu, estendendo o teste existente do drawer/menu de `MainLayout` se houver): verificar que DEVELOPER EXPERIENCE é a primeira seção renderizada no drawer; verificar que "New Application" aparece dentro dessa seção, abaixo de "Templates Catalog"; verificar que não existe mais um item avulso "New Application" fora de seção.
