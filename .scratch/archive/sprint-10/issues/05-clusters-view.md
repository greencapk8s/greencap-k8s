# 05 — Traduzir ClustersView

Status: done

## Contexto

ClustersView tem mais strings do que as demais views: botões, dialogs, field labels, tooltips, hints e notificações.

## O que fazer

Grid e toolbar:
- `"Adicionar Cluster"` → `"Add Cluster"`
- `"Ativo"` (coluna) → `"Active"`
- `"Nome"` (coluna) → `"Name"`

Tooltips de ações:
- `"Testar conexão"` → `"Test connection"`
- `"Remover cluster"` → `"Remove cluster"`

Dialog de remoção:
- Header `"Remover cluster"` → `"Remove cluster"`
- Botão `"Remover"` → `"Remove"`

Dialog de adição:
- Campo `"Nome"` → `"Name"`
- Error message `"Nome é obrigatório"` → `"Name is required"`
- `"Erro ao ler arquivo"` → `"Error reading file"`
- Hint do kubeconfig (português) → traduzir para inglês mantendo o comando kubectl
- Notificação de sucesso: `"conectado com sucesso"` → `"connected successfully"`
- Notificação de adição sem conexão: `"adicionado (sem conexão — verifique o kubeconfig)"` → `"added (no connection — check the kubeconfig)"`

## Critério de aceite

- Todos os textos visíveis ao usuário em inglês
- Fluxo de adicionar e remover cluster funciona normalmente
- Compilação sem erros
