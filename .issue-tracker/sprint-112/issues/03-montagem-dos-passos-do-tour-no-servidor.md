# 03 — Montagem dos passos do Tour no servidor

Status: done

Consome as issues 01 e 02. É o coração da sprint.

O Tour é definido no Java e entregue pronto ao navegador. A alternativa — uma lista estática no TypeScript — foi descartada por dois motivos registrados no ADR 0024: o texto é conteúdo de produto, que vai ser reescrito muitas vezes e fica melhor junto do resto da interface; e, decisivo, o projeto não tem nenhuma infraestrutura de teste de frontend, então tudo que mora no TypeScript nasce sem cobertura possível. Com a montagem no servidor, a lógica que mais erra fica alcançável por Karibu.

Surge um `TourComponent`, seguindo exatamente o contrato que o `TopologyGraphComponent` já estabeleceu no projeto: um componente Java com tag própria, apontando para o módulo TypeScript da issue 04 e declarando a dependência npm, entregando dados ao cliente como propriedade serializada e recebendo o retorno por método invocável pelo cliente. Não há mecanismo novo a inventar.

Ele vive no `MainLayout`, não numa view. Quatro dos seis passos miram elementos que o próprio `MainLayout` constrói — header e seções do menu —, e uma view filha teria que alcançar o layout pai para anotá-los; além disso o Tour morreria em qualquer navegação. No layout, ele acompanha a sessão inteira.

São seis passos, na ordem em que o olho percorre a tela: o Cluster e o Namespace ativos no header, que são o contexto do qual tudo o mais depende; Developer Experience, apresentando Templates Catalog e New Application; Topology, a visão que diferencia o produto; Project, o dia a dia de Workloads, Networking e Storage; Global e Settings, com Clusters, Namespaces e Registry; e o fecho, contando que o que está na tela é o próprio GreenCap rodando neste cluster — a plataforma se gerenciando, que é a demonstração mais direta possível para quem está começando.

O passo de Settings menciona Users apenas para o `PlatformAdmin`. Não é filtragem por RBAC: o menu do projeto não é filtrado por permissão, e todos os itens são renderizados para todo mundo. É que a view de Users tem guard próprio e devolve o usuário não-admin ao Dashboard com um aviso — apresentá-la a quem não pode entrar seria ensinar um beco sem saída. É a única condicional da lista, e ela é resolvida no servidor, que já sabe quem está logado.

Os textos são escritos em inglês, como o resto da interface do produto.

O disparo é responsabilidade desta issue. Na entrada da sessão, o `MainLayout` consulta a preferência da issue 02 e o Cluster ativo, e só inicia quando o Tour ainda não foi visto e existe Cluster. Antes de iniciar, abre o drawer — em viewport estreita o Vaadin o mantém recolhido, e sem essa abertura quatro dos seis passos mirariam elementos invisíveis, encolhendo o Tour justamente para quem tem a tela menor. A largura persistida do drawer não é alterada, apenas o estado aberto.

A conclusão chega do navegador por chamada do cliente e grava a preferência. Como o overlay do Driver.js bloqueia interação, o usuário não consegue navegar no meio do Tour, então não existe o caso de perder o fio entre views.

Cobertura de teste: Karibu estendendo `KaribuTest` — a lista sai com os seis passos para o `PlatformAdmin` e sem a menção a Users para o não-admin; cada passo aponta para um identificador que existe entre as constantes da issue 01; o Tour não é iniciado quando a preferência já está marcada; e não é iniciado quando não há Cluster ativo, mesmo com a preferência falsa.

Fora de escopo: a renderização em si, que é a issue 04. Aqui o componente apenas entrega a lista e recebe a confirmação.

## Comments

**15/08/2026** — Implementada. `TourStep`, `TourSteps` (os seis passos, texto em inglês, condicional
de Users via `SecurityUtils.isAdmin()`) e `TourComponent`, seguindo o contrato do
`TopologyGraphComponent`. Gatilho em `MainLayout.startTourIfFirstAccess()`, chamado pelo `onAttach`.
Sete testes Karibu. Pendente o aceite manual.

O mapeamento dos seis passos nos seis alvos exigiu uma leitura: esta issue enumera "Global e
Settings" como um passo só e um fecho sem alvo, o que usaria cinco alvos, enquanto a issue 01 supõe
que os seis são usados. Global e Settings viraram passos próprios e o fecho encerra o de Settings.

**22/09/2026** — Aceite manual concluído. A leitura de seis passos para seis alvos, com o fecho
no passo de Settings, foi revisada com o Tour rodando e mantida.
