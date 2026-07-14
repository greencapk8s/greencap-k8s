---
title: "Ícone do menu Helm — símbolo ⎈ (leme) em SVG"
status: done
priority: medium
sprint: 92
---

Substituir o ícone atual do item de menu Helm (`VaadinIcon.PACKAGE`) pelo símbolo ⎈ (U+2388 — HELM SYMBOL), que é a logo oficial do Helm.

O SVG do símbolo deve ser salvo em `src/main/resources/META-INF/resources/icons/helm.svg` e carregado no `MainLayout` via `SvgIcon`, seguindo o mesmo padrão do `greencap.png`. O ícone deve herdar a cor do texto da sidebar para se comportar igual aos demais ícones Vaadin em qualquer tema.
