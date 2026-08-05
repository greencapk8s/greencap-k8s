# 02 — Transporte do Build Context local até o Job Kaniko

Status: done

A contraparte da issue 01 do lado do cluster: fazer o contexto empacotado chegar ao Kaniko. Hoje `RegistryService.startBuild` monta o Job com contexto Git e o próprio Pod clona o repositório — o GreenCap nunca transfere byte nenhum. Esta entrega abre um segundo caminho, mantendo o primeiro intacto.

`BuildRequest` hoje descreve uma origem só, por campos de Git (URL, branch, context path, dockerfile path). Precisa passar a expressar **duas** origens de Build Context sem que os quatro call sites existentes precisem mudar de comportamento — três deles (Registry, Deploy Template e os wizards no estado atual) continuam usando Git. O desenho deve tornar impossível construir um request ambíguo, com Git e pasta local ao mesmo tempo.

O mecanismo de transporte, decidido na ADR 0020: o Job Kaniko ganha um `initContainer` que compartilha um volume efêmero com o container do Kaniko e fica bloqueado esperando um arquivo sentinela aparecer. Com o Pod já em execução, o GreenCap injeta o contexto empacotado nesse initContainer pela API de exec do Kubernetes — o mesmo mecanismo do `kubectl cp`, já exposto pelo Fabric8 — e em seguida grava a sentinela. O initContainer então termina, e o container do Kaniko sobe apontando para o arquivo local em vez de para uma URL Git.

Nem ConfigMap nem PersistentVolumeClaim entram nisso, e a razão importa para quem for mexer depois: ConfigMap tem teto rígido de cerca de 1 MiB no etcd, e um PVC exigiria a mesma API de exec de qualquer forma, somando uma StorageClass e um volume a limpar para guardar um artefato que vive segundos.

O ponto delicado é a coreografia: a injeção só pode acontecer depois que o Pod está em execução, e o build só pode começar depois que a injeção terminou. A espera pelo Pod precisa de um limite de tempo com erro legível — um Pod que nunca agenda (nó sem recursos, imagem não baixável) não pode deixar a tela pendurada indefinidamente. Falha em qualquer etapa deve encerrar o Job em vez de deixá-lo bloqueado no initContainer até o TTL expirar.

Esta entrega traz uma exigência de RBAC nova: criar `pods/exec` no namespace de build. Contas sem essa permissão recebem 403, que o `KubernetesOperationException` já traduz para uma mensagem legível sobre permissão. Não haverá verificação prévia — o projeto não tem esse padrão desde que a ADR 0013 removeu as checagens antecipadas, e introduzi-lo para um caso só seria abstrair antes da segunda ocorrência. O que esta entrega deve garantir é que a falha apareça de forma compreensível e no momento em que ocorre, não como erro genérico de build.

Cobertura de teste: testes de integração do `RegistryService` cobrindo a montagem do Job com origem local (presença do initContainer, do volume compartilhado e dos argumentos do Kaniko apontando para o arquivo em vez de URL Git), a montagem inalterada com origem Git, e a rejeição de um `BuildRequest` malformado. O caminho de exec real depende de um Pod em execução e será exercitado no aceite manual, via issue 03.

Fora de escopo: as telas que acionam este caminho (issues 03 e 04) e o dialog de Build da view de Registry, que continua só com Git — follow-up registrado no backlog.
