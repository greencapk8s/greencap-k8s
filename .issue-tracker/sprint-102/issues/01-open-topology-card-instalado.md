# 01 — Open Topology no card de Template instalado

Status: done

No Templates Catalog, quando um Template já está instalado no Cluster ativo, o card hoje mostra apenas o badge "Installed" e não oferece nenhuma forma de entrar na solução implantada. Esta entrega adiciona ao card instalado uma ação "Open Topology" que leva o usuário direto para a Topologia da solução — o mesmo contrato de navegação do pós-deploy que já existe em Deploy Template e nos fluxos de New Application.

Comportamento ao clicar em "Open Topology": trocar o Namespace ativo para o Namespace do Template (que já vem no `TemplateSummary`, lido do `catalog.json` — sem fetch adicional), persistir esse Namespace como o Namespace ativo do usuário, atualizar o combobox de Namespaces do header (mesmo mecanismo `MainLayout.refreshNamespaceSelector(UI)` extraído na Sprint 101) e navegar para a `TopologiaView`. Como a Topologia é namespace-scoped e renderiza o Namespace ativo, essa troca de contexto é intencional e visível: o header passa a apontar para a Namespace da solução. É navegação, não uma operação de escrita — sem gate de permissão (ADR 0013).

O estado "Installed" é um snapshot calculado no load/refresh da view. A ação navega sem re-checar a existência da Namespace no clique: se a solução foi removida por fora nesse meio-tempo, a `TopologiaView` (já async, com tratamento de inacessível desde a Sprint 50) mostra uma topologia vazia, sem crash — resultado honesto, consistente com o tratamento de snapshot de `ConnectionStatus` e do próprio badge.

Layout do card instalado: o badge "Installed" fica à esquerda e o botão "Open Topology" à direita (footer com os dois lados ocupados, em vez do alinhamento à direita atual só com o badge). O botão é secundário e discreto (small, tertiary), com o mesmo ícone usado pela Topologia no sidebar, para não competir visualmente com o "Deploy" primário dos cards não-instalados.

Fix cosmético incluído nesta mesma entrega: no badge "Installed", o ícone de check e a palavra "Installed" estão colados hoje — adicionar um espaçamento entre eles.

Glossário (`CONTEXT.md`) já atualizado na etapa de planejamento: a entrada Templates Catalog passa a descrever a ação "Open Topology" no card instalado, deixando explícito que é distinta do botão "Go to resource" do painel de detalhe da própria Topologia.

Cobertura de teste (Karibu, estendendo `SampleCatalogViewTest`): um card instalado renderiza o botão "Open Topology" junto ao badge "Installed"; clicar no botão troca o Namespace ativo para o Namespace do Template e navega para a `TopologiaView`; um card não-instalado não mostra o botão (mostra o "Deploy") — guard do estado instalado.

Fora de escopo (registrado no backlog para sprint própria): ação de Uninstall no card — operação destrutiva com design tree próprio (o que "uninstall" realmente remove, imagens no Registry interno, confirmação type-to-confirm, hierarquia visual do footer).
