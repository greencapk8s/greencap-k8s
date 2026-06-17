# Karibu-Testing: testes de dialogs destrutivos

Status: done

## Contexto

A camada de serviços está coberta por testes de integração com TestContainers (sprint 81). A camada de views não tem cobertura alguma. Os dialogs destrutivos são o ponto de maior risco: um comportamento errado pode apagar um namespace inteiro ou remover o acesso a um cluster.

## Entrega

Configurar o Karibu-Testing no projeto e cobrir os dois dialogs destrutivos de maior blast radius com testes automatizados.

## Setup esperado

Nova classe base `KaribuTest` com `@SpringBootTest` sem TestContainers. Serviços mockados via `@MockBean`. Permissões configuradas com `@WithMockUser(authorities = {Permission.XXX.name()})` para garantir type safety.

## Cenários

Cinco cenários distribuídos em duas classes de teste:

**`NamespacesViewTest`**

- Clicar Delete com `kube-system` selecionado exibe notificação de erro e não abre nenhum dialog.
- Clicar Delete com namespace regular abre o dialog com o botão Delete desabilitado.
- Digitar um nome errado no confirm field mantém o botão Delete desabilitado.
- Digitar o nome exato do namespace habilita o botão Delete.

**`ClustersViewTest`**

- Confirmar a remoção de um cluster chama `clusterService.deleteCluster()` com o cluster correto.

## Dependência a adicionar

`com.github.mvysny.kaributesting:karibu-testing-v24` versão compatível com Vaadin 24.4.
