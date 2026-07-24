# 03 — DeploymentsView: botão Rollout Undo

## Status
closed

## Descrição
Adicionar botão "Rollout Undo" na coluna de ações do `DeploymentsView`, protegido por
`WORKLOADS_DEPLOYMENTS_ROLLBACK`. Exibe dialog de confirmação antes de executar.
Notificação de sucesso/erro em `BOTTOM_END`.

## Critérios de aceite
- [ ] Botão "Rollout Undo" visível na coluna de ações
- [ ] Botão desabilitado quando usuário não tem `WORKLOADS_DEPLOYMENTS_ROLLBACK`
- [ ] Dialog de confirmação antes de executar o undo
- [ ] Notificação de sucesso após undo
- [ ] Notificação de erro se `KubernetesOperationException`
- [ ] Grid recarregado após undo bem-sucedido
- [ ] Largura da coluna de ações ajustada para acomodar o novo botão
