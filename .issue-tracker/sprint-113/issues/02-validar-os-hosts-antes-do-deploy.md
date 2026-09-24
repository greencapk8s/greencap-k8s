# 02 — Validar os hosts antes do Deploy

Status: todo
Blocked by: 01

O Import Compose passa a ser o primeiro wizard com vários hosts na mesma tela, e isso cria um risco que o Deploy Application e o Deploy from Dockerfile não têm: dois serviços com o mesmo host. Nenhum wizard valida host hoje; esta issue valida no Import Compose.

Ao clicar em Deploy no passo 2, o host de cada serviço marcado para exposição é verificado, e o Deploy é bloqueado quando ele:

- está vazio;
- está fora do formato de nome DNS — letras minúsculas, dígitos, hífens e pontos, cada parte começando e terminando com letra ou dígito, até 253 caracteres;
- repete o host de outro serviço marcado neste mesmo Compose — o erro aparece nos dois campos.

Havendo qualquer erro, o wizard não avança para a execução e a mensagem aparece no próprio campo, em inglês como o resto da interface. Um serviço desmarcado não participa da validação, mesmo que o seu host, agora escondido, tenha ficado inválido. Corrigidos os hosts, o Deploy segue normalmente.

Conflito com Ingress de outro Namespace não é validado aqui: chega como erro da API e cai no tratamento de falha da issue 01.

## Critérios de aceite

- [ ] Host vazio, fora do formato DNS ou repetido entre serviços marcados bloqueia o Deploy, com a mensagem no campo
- [ ] Um serviço desmarcado não participa da validação
- [ ] Com hosts válidos e distintos, o Deploy segue para a execução
- [ ] No browser: expor `api` e `nginx` do compose de demo com o mesmo host e ver o Deploy bloqueado nos dois campos

## Testes

Karibu no `ImportComposeViewTest`, na mesma seam da issue 01: cada caso de bloqueio impede a chamada ao provisionamento e marca o campo como inválido; hosts válidos e distintos chegam ao provisionamento; um serviço desmarcado com host inválido não bloqueia.

## Fora de escopo

- Validação de host no Deploy Application e no Deploy from Dockerfile — registrada no backlog
