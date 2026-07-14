---
title: "Substituir TextAreas de YAML pelo CodeMirrorEditor nas views"
status: done
priority: high
sprint: 92
---

Substituir os `TextArea` usados para edição de YAML em três views pelo componente `CodeMirrorEditor` criado na issue 01.

As views afetadas são `ManifestView` (editor principal de manifests), `DeployFromHelmView` (campo values.yaml no wizard de deploy) e `HelmReleasesView` (painel de values no dialog de detalhes, que começa em leitura enquanto carrega e transita para editável).

`ClustersView` mantém o `TextArea` existente para kubeconfig — operação de paste único sem benefício de editor.

Toda a lógica de negócio das views permanece inalterada; apenas o componente de entrada de texto muda.
