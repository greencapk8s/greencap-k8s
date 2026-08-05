# Mode selector — adicionar botão "Deploy from Dockerfile"

Status: done

O seletor de modos de deploy exibido no topo de `DeployApplicationView` e `ImportComposeView` precisa incluir o terceiro modo. A ordem definida é `[Deploy from Image] [Deploy from Dockerfile] [Deploy from Compose]`. O botão correspondente ao modo ativo fica com variante `LUMO_PRIMARY`; os demais com `LUMO_TERTIARY`. O novo botão navega para a rota `/deploy/dockerfile` (view a ser criada na issue 02).
