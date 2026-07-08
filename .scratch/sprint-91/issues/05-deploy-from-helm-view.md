---
id: "91-05"
title: "DeployFromHelmView — wizard 3 passos e modo selector"
status: done
priority: high
sprint: 91
---

`DeployFromHelmView` na rota `deploy/helm`, protegida por `PROJECT_HELM_INSTALL`. Wizard 3 passos seguindo o padrão visual das demais views de deploy (step indicator, botões Back/Next/Install, max-width 820px):

Step 1 — Chart: ComboBox de repositórios configurados para o cluster ativo; TextField de chart name; TextField de versão (placeholder "latest").

Step 2 — Config: TextField de release name (auto-sugerido a partir do chart name ao digitar, convertendo para lowercase e substituindo caracteres inválidos); TextField de namespace (pré-preenchido com namespace ativo, editável).

Step 3 — Values & Install: TextArea YAML para values customizados (placeholder com exemplo de YAML); botão "Install" no lugar de "Next"; após clicar, exibe inline o output do `helm install` em componente `Pre`; em sucesso navega para `HelmReleasesView`; em falha exibe erro inline.

`DeployApplicationView`: adiciona quarto botão "Deploy from Helm" (com `VaadinIcon.PACKAGE`) ao modo selector, navegando para `DeployFromHelmView`.
