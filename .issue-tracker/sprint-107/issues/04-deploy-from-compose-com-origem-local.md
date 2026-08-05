# 04 — Deploy from Docker Compose aceita pasta local como origem

Status: done

A segunda superfície da sprint, com o mesmo desenho do fluxo de Dockerfile: uma escolha de origem na etapa 1, um botão que abre o seletor nativo de pastas, e o caminho do arquivo continuando como campo de texto. Origem Git permanece o padrão e inalterada.

O campo de caminho do Compose vale nas duas origens, agora relativo à raiz da pasta selecionada em vez da raiz do repositório. É por ele que o usuário aponta um arquivo cujo nome ou localização fujam do padrão — `compose.yaml` em vez de `docker-compose.yml`, ou um arquivo dentro de um subdiretório. O default cobre o caso comum sem ninguém digitar nada.

Com a pasta selecionada, os serviços que buildam resolvem-se todos a partir do mesmo contexto: cada `build:` aponta para um subdiretório dentro da árvore que subiu. Um upload serve todos os serviços que precisam buildar, sem mecanismo adicional — a resolução de caminho já existente, que compõe o diretório do compose com o contexto declarado no serviço, passa a valer sobre a raiz da pasta em vez da raiz do repositório.

Trocar de origem não pode deixar resíduo: alternar entre Git e Pasta Local precisa limpar o estado da origem abandonada, para que não seja possível disparar um deploy com uma pasta selecionada e uma URL Git preenchida ao mesmo tempo.

O restante do fluxo permanece: review com PVCs e imagens editáveis, avisos de diretivas ignoradas, builds sequenciais com log ao vivo, provisionamento e navegação para a Topologia.

**Selecionar o arquivo Compose isolado ficou fora, por decisão tomada durante o aceite manual.** O desenho original oferecia essa segunda forma pensando em quem copia um compose de tutorial que só referencia imagens públicas: não há nada para buildar, e exigir uma pasta inteira para um arquivo de quinze linhas parecia atrito gratuito. Na prática ela custava mais do que economizava — era o único ponto da plataforma onde um Build Context era opcional, e produzia um estado de tela em que serviços com `build:` não tinham como ser buildados, estado que só se resolvia com um aviso acionável e um bloqueio de deploy existindo unicamente por causa dessa forma. Alinhar as duas telas eliminou o estado inteiro. O custo aceito é que quem tem um compose solto numa pasta grande empacota a pasta inteira; o resumo pré-envio e o teto de 50 MB tornam isso visível antes de qualquer envio.

Cobertura de teste: testes Karibu cobrindo a alternância entre Git e Pasta Local sem estado residual, a permanência do campo de caminho na origem local, a recusa de avanço com Pasta Local escolhida e nenhuma pasta selecionada, e a sugestão do namespace a partir do nome da pasta. Um teste de unidade do parsing cobre que um compose lido de conteúdo local produz o mesmo resultado que o mesmo conteúdo vindo de Git, já que o parser antes só era alcançado pelo caminho de busca HTTP. Builds reais são aceite manual.

Fora de escopo: Ingress por serviço no Compose (item já no backlog desde a Sprint 83) e o `.dockerignore` (issue 05).
