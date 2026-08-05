# 03 — Deploy from Dockerfile aceita pasta local como origem

Status: done

Primeira entrega ponta a ponta da sprint, e a que prova o mecanismo inteiro: selecionar uma pasta no computador, buildar e ver a aplicação rodando no cluster, sem publicar nada num host Git. Consome as issues 01 e 02.

A etapa 1 do wizard (`Source & Name`) passa a oferecer uma escolha de origem entre Repositório Git e Pasta Local, com **Git como padrão** — o fluxo existente não muda para quem já o usa, e a origem Git continua sendo a mais forte das duas por ser versionada e reproduzível.

Ao escolher Pasta Local, os campos da etapa mudam de sentido. `Branch` deixa de existir, porque não há ref a resolver. O `Dockerfile path` continua, agora relativo à raiz da pasta selecionada em vez da raiz do repositório, mantendo o padrão `Dockerfile` quando em branco. O `Context path` continua, ainda designando um subdiretório dentro do contexto — o significado é o mesmo, só a raiz muda. O nome da aplicação, hoje sugerido a partir do nome do repositório, passa a ser sugerido a partir do nome da pasta selecionada, seguindo as mesmas regras de saneamento para nome de Namespace.

Trocar de origem não pode deixar resíduo: alternar entre Git e Pasta Local precisa limpar o estado da origem abandonada, para que não seja possível disparar um build com uma pasta selecionada e uma URL Git preenchida ao mesmo tempo.

A detecção da porta exposta pelo Dockerfile, que hoje acontece ao avançar da etapa 1 buscando o arquivo cru pela HTTP do host Git, precisa de um caminho equivalente para a origem local, lendo o Dockerfile a partir do que foi selecionado. O comportamento visível deve ser o mesmo nas duas origens: a porta sugerida na etapa 2 quando encontrada, e o campo em branco quando não. Falha na detecção nunca bloqueia o avanço — o usuário informa a porta manualmente.

A etapa de Review deve descrever a origem de forma honesta em vez de reaproveitar o texto de Git: nome da pasta, quantidade de arquivos e tamanho do contexto, no lugar de URL e branch. Quem confirma precisa reconhecer o que está prestes a buildar.

A partir da confirmação, nada mais muda: build com log ao vivo, provisionamento de Namespace, Deployment e opcionalmente Service, PVC e Ingress, navegação para a Topologia em caso de sucesso, erro inline sem rollback em caso de falha. Toda a diferença entre as origens termina no momento em que o Job Kaniko começa.

Cobertura de teste: testes Karibu cobrindo a alternância de origem (campos de Git escondidos ao escolher Pasta Local e vice-versa, sem estado residual), a validação da etapa 1 recusando o avanço com Pasta Local escolhida e nenhuma pasta selecionada, e a sugestão do nome da aplicação a partir do nome da pasta. O build real, o exec e a subida da aplicação são aceite manual.

Fora de escopo: o fluxo de Compose (issue 04) e o `.dockerignore` (issue 05), que se aplicará a esta tela automaticamente quando entregue.
