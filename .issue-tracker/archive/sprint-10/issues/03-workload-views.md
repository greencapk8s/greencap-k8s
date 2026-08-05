# 03 — Traduzir PodsView e DeploymentsView

Status: done

## O que fazer

PodsView — colunas:
- `"Nome"` → `"Name"`
- `"Namespace"` → manter (termo de domínio)
- `"Status"` → manter
- `"Node"` → manter
- `"Restarts"` → manter
- `"Idade"` → `"Age"`

DeploymentsView — colunas:
- `"Nome"` → `"Name"`
- `"Namespace"` → manter
- `"Desired"` / `"Ready"` / `"Available"` → verificar se já estão em inglês
- `"Idade"` → `"Age"`

Ambas as views:
- Usar `UiConstants.buildNoClusterMessage()` no lugar do método local (depende da issue 01)
- Botão `"Ir para Clusters"` removido (movido para UiConstants)

## Critério de aceite

- Todas as colunas exibem headers em inglês
- Mensagem de sem cluster vem de `UiConstants`
- Compilação sem erros
