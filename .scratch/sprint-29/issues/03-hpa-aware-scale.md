# HPA-aware Scale + edição de limites do HorizontalScaler

Status: done

## Descrição

Se um Deployment tem um HPA associado, o botão Scale na DeploymentsView deve navegar para a HorizontalScalerView e abrir automaticamente o dialog de edição de limites (min/max réplicas) daquele HPA, em vez de abrir o dialog de scale direto no Deployment.

A HorizontalScalerView ganha um botão de edição por linha que abre o mesmo dialog.

## Critérios de aceite

- [ ] `AutoScalingService.findHorizontalScalerForDeployment()` localiza o HPA pelo `scaleTargetRef.name`
- [ ] `AutoScalingService.updateHorizontalScaler()` atualiza min/max via Fabric8 edit
- [ ] Scale em DeploymentsView: se HPA existe → navega para `/autoscaling/horizontalscalers?edit=<hpa-name>`
- [ ] Scale em DeploymentsView: se sem HPA → abre dialog de scale normal (comportamento existente)
- [ ] HorizontalScalerView lê o query param `edit` e abre o dialog automaticamente
- [ ] Dialog de edição do HPA: IntegerField min (≥ 1) e max (≥ min), botões Save e Cancel
- [ ] Sucesso: notificação `BOTTOM_END` + refresh do grid
- [ ] Falha: notificação de erro `BOTTOM_END`
