# 04 — Traduzir ServicesView, ConfigMapsView e SecretsView

Status: done

## O que fazer

ServicesView — colunas:
- `"Nome"` → `"Name"`
- `"Tipo"` → `"Type"`
- `"Cluster IP"` → manter
- `"Porta(s)"` → `"Port(s)"`
- `"Namespace"` → manter
- `"Idade"` → `"Age"`

ConfigMapsView — colunas:
- `"Nome"` → `"Name"`
- `"Keys"` → manter
- `"Namespace"` → manter
- `"Idade"` → `"Age"`

SecretsView — colunas:
- `"Nome"` → `"Name"`
- `"Tipo"` → `"Type"`
- `"Keys"` → manter
- `"Namespace"` → manter
- `"Idade"` → `"Age"`

Todas as três views:
- Usar `UiConstants.buildNoClusterMessage()` no lugar do método local (depende da issue 01)

## Critério de aceite

- Todas as colunas exibem headers em inglês
- Mensagem de sem cluster vem de `UiConstants`
- Compilação sem erros
