# 03 — A Topologia reflete o PodState nos nós de Pod e PodGroup

Status: done

Fecha a sprint eliminando o mesmo defeito na outra tela onde ele aparece. Consome a issue 01.

Na Topologia de hoje, os nós de Deployment, StatefulSet e ReplicaSet já dizem a verdade: derivam status de réplicas prontas contra desejadas, e por isso mostram `Degraded` corretamente. Só os nós de Pod e o nó sintético de PodGroup usam a fase — o resultado é um Deployment marcado como degradado com um grupo de Pods "Running" logo abaixo, a mesma contradição que aparece entre a listagem de Deployments e a de Pods.

O nó de Pod passa a carregar o `PodState`, exatamente o mesmo valor que a listagem exibe para aquele Pod. As duas telas precisam dizer a mesma palavra sobre o mesmo recurso; divergir seria trocar um bug por uma inconsistência.

No nó de PodGroup, a causa vence: havendo qualquer Pod problemático no grupo, o nó assume o `PodState` dele, escolhido pelo primeiro em ordem de nome para ser determinístico; com todos saudáveis, `Running`. Isso aposenta `Degraded` e `Failed` nesse nó específico, e é ganho, não perda. `Degraded` dizia que algo não batia sem dizer o quê, e `Failed` é menos informativo que a razão real por trás dele. O par de nós passa a se ler junto: o controlador acima responde quantos estão prontos, o grupo abaixo responde por quê. `Degraded` permanece intocado nos nós de controlador, onde é bem fundado.

A ressalva aceita: com uma réplica quebrada em três, o grupo mostra a razão da quebrada, o que soa mais grave do que é. Preferimos errar para esse lado — subnotificar é o defeito que a sprint existe para corrigir, e o nó do controlador ao lado dá a proporção.

A severidade precisa atravessar para o frontend, porque o grafo hoje tem o mesmo defeito espelhado em TypeScript: uma tabela que mapeia texto de status para cor de borda, com fallback cinza. Um nó `ImagePullBackOff` cairia nesse fallback — palavra certa, borda cinza. Enquanto essa tabela existir, toda razão nova do Kubernetes precisará ser lembrada em dois lugares, e o esquecimento é silencioso.

Então o nó do grafo passa a carregar a severidade decidida no servidor, o frontend colore por ela, e a tabela por texto de status é removida. São as quatro severidades: saudável em verde, degradada em âmbar, problema em vermelho, neutra em cinza. A degradada existe justamente para preservar o âmbar atual dos controladores — sem ela, "uma réplica pronta de três" viraria vermelho e perderíamos a distinção entre degradação parcial e falha, que hoje funciona e não é alvo da sprint. Os demais tipos de nó são mapeados para preservar exatamente a cor que já têm, para que nenhum nó mude de aparência sem decisão: Service e Ingress ficam em saudável, porque o status `Active` que eles carregam já os pintava de verde pela tabela antiga; PersistentVolumeClaim fica em neutra, porque nenhum de seus status (`Bound`, `Terminating`, `Lost`) constava da tabela e todos caíam no cinza do fallback.

Por fim, a borda fica mais espessa quando a severidade é de problema. Isso é uma mitigação consciente, não a solução completa: no nosso grafo o corpo do nó é colorido por tipo de recurso, então uma borda fina de estado disputa atenção com um fundo saturado e o alarme sai fraco. O modelo do OpenShift — corpo neutro, tipo em ícone, estado dominando pelo anel — está registrado no backlog como sprint própria, porque trocar o canal de codificação do tipo é redesenho, não ajuste de cor, e misturá-lo aqui faria o aceite manual validar duas coisas ao mesmo tempo.

Cobertura de teste: extensão dos testes existentes de Topologia, cobrindo o nó de Pod carregando o mesmo `PodState` da listagem, o nó de PodGroup assumindo a razão do Pod problemático com uma réplica quebrada em três, o grupo inteiramente saudável voltando a `Running`, e a severidade acompanhando cada caso — incluindo um nó de controlador degradado permanecendo em âmbar, que é a regressão mais provável desta issue.

Fora de escopo: o redesenho dos nós, a forma circular, e qualquer mudança no painel de detalhe que abre ao clicar num nó.
