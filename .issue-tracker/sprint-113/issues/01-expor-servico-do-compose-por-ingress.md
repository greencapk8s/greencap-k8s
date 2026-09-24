# 01 — Expor um serviço do Compose por Ingress

Status: todo
Blocked by: nenhuma — pode começar já

Hoje o Import Compose traduz `ports:` num Service ClusterIP e para aí: um serviço importado só é alcançável de dentro do cluster. Esta issue leva o caminho inteiro, da tela de revisão ao recurso criado, para que o usuário exponha um serviço externamente por Ingress, no mesmo modelo que já conhece do Deploy Application.

**Na tela de revisão (passo 2).** Cada serviço que tem `ports:` mostra no seu painel a opção "Expose application externally (Ingress)", desmarcada. Só ele, porque só ele ganha Service, e Ingress precisa de Service; um serviço sem `ports:` não mostra a opção. Ao marcar, aparecem dois campos no próprio painel:

- Host, sugerido como `<service>.<namespace>.greencap.local` — o nome do serviço normalizado como nos demais recursos, e o Namespace escolhido no passo 1 — e editável.
- IngressClass, com as classes disponíveis no cluster e a primeira pré-selecionada.

Ao desmarcar, os campos somem e o serviço não é exposto. Como os outros campos do passo 2, o que foi editado se perde ao voltar ao passo 1 — comportamento atual, mantido.

**No provisionamento.** Para cada serviço marcado, depois do Service, é criado um Ingress `<service>-ingress` no Namespace do import, com uma regra para o host informado, path `/` do tipo `Prefix`, backend no Service do serviço, na primeira porta de `ports:` — a mesma que o Service carrega —, a IngressClass escolhida e os mesmos labels `app.kubernetes.io/part-of` e `app.kubernetes.io/component` dos demais recursos, para aparecer agrupado na Topologia. Um serviço não marcado não ganha Ingress. O Ingress criado entra na lista de recursos do resultado daquele serviço.

**Falha.** O Ingress segue o tratamento dos demais recursos do Import Compose: se a criação falhar, o resultado daquele serviço mostra "Ingress failed" com a mensagem da API e lista o que já foi criado, sem rollback, e os outros serviços seguem. Se o Service falhar antes, o Ingress não é tentado. O caso real é o controller recusar um host já usado por Ingress de outro Namespace.

**Por que assim** (decidido no planejamento e registrado no `CONTEXT.md`, em Import Compose): um Ingress por serviço, nunca um Ingress único com path rules — um prefixo de path quebra aplicações que esperam rodar na raiz, e o rewrite que corrigiria isso é anotação específica de cada controller. Só a primeira porta, porque é a única que o Service carrega; levar múltiplas portas ao Service é outra mudança.

## Critérios de aceite

- [ ] Serviços com `ports:` mostram a opção desmarcada; serviços sem `ports:` não a mostram
- [ ] Marcar mostra o host sugerido `<service>.<namespace>.greencap.local` e a IngressClass com a primeira classe do cluster pré-selecionada; desmarcar esconde os dois
- [ ] O deploy cria `<service>-ingress` só para os serviços marcados, com host, classe, path `/` do tipo `Prefix`, backend no Service na primeira porta e os labels da Topologia
- [ ] Uma falha na criação do Ingress aparece como "Ingress failed" no resultado do serviço, com os recursos já criados listados
- [ ] Ponta a ponta: importar o compose de `.dev/greencap-demo`, expor a `api`, ver o Ingress agrupado na Topologia e abrir `api.<namespace>.greencap.local` com a linha correspondente no `/etc/hosts`

## Testes

Esta sprint experimenta escrever os testes dentro da implementação, em red→green, nas seams combinadas no planejamento — em vez de deixá-los para depois do aceite.

- **Provisionamento**, com o cliente mock do Fabric8 no mesmo modelo do `WorkloadServiceTest`, numa classe de teste nova para o `ImportComposeService`: um serviço marcado ganha o Ingress com host, classe, backend, porta e labels; um serviço não marcado não ganha Ingress; um Ingress que já existe no Namespace produz "Ingress failed" com a lista do que foi criado.
- **Wizard**, com Karibu no `ImportComposeViewTest`, chegando ao passo 2 sem depender de rede: a opção aparece só nos serviços com `ports:` e começa desmarcada; o host é sugerido; a requisição enviada ao provisionamento leva o Ingress só dos serviços marcados.

## Fora de escopo

- Ingress único com path rules por serviço
- Múltiplas portas no Service
- Orientação sobre `/etc/hosts` no wizard — o item "Acesso local via `*.greencap.local`" do backlog trata isso para todos os wizards
- Validação do host — issue 02
