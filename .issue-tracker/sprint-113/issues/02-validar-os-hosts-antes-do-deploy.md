# 02 — Validar os hosts antes do Deploy

Status: done
Blocked by: 01

O Import Compose passa a ser o primeiro wizard com vários hosts na mesma tela, e isso cria um risco que o Deploy Application e o Deploy from Dockerfile não têm: dois serviços com o mesmo host. Nenhum wizard valida host hoje; esta issue valida no Import Compose.

Ao clicar em Deploy no passo 2, o host de cada serviço marcado para exposição é verificado, e o Deploy é bloqueado quando ele:

- está vazio;
- está fora do formato de nome DNS — letras minúsculas, dígitos, hífens e pontos, cada parte começando e terminando com letra ou dígito, até 253 caracteres;
- repete o host de outro serviço marcado neste mesmo Compose — o erro aparece nos dois campos.

Havendo qualquer erro, o wizard não avança para a execução e a mensagem aparece no próprio campo, em inglês como o resto da interface. Um serviço desmarcado não participa da validação, mesmo que o seu host, agora escondido, tenha ficado inválido. Corrigidos os hosts, o Deploy segue normalmente.

Conflito com Ingress de outro Namespace não é validado aqui: chega como erro da API e cai no tratamento de falha da issue 01.

## Critérios de aceite

- [x] Host vazio, fora do formato DNS ou repetido entre serviços marcados bloqueia o Deploy, com a mensagem no campo
- [x] Um serviço desmarcado não participa da validação
- [x] Com hosts válidos e distintos, o Deploy segue para a execução
- [x] No browser: expor `api` e `nginx` do compose de demo com o mesmo host e ver o Deploy bloqueado nos dois campos

## Testes

Karibu no `ImportComposeViewTest`, na mesma seam da issue 01: cada caso de bloqueio impede a chamada ao provisionamento e marca o campo como inválido; hosts válidos e distintos chegam ao provisionamento; um serviço desmarcado com host inválido não bloqueia.

## Fora de escopo

- Validação de host no Deploy Application e no Deploy from Dockerfile — registrada no backlog

## Comments

**24/09/2026** — Implementada, com os testes escritos em red→green. A validação fica no
`ComposeServiceExposure`, criado na issue 01, e roda quando o usuário clica em Deploy no passo 2.
Antes de validar, ela limpa o erro de todos os serviços, então um serviço que foi desmarcado não
guarda um erro antigo para quando voltar a ser marcado. O host é comparado já sem espaços nas
pontas, o mesmo valor que vai no request. Um host só de espaços conta como vazio.

Os testes estão em `ImportComposeViewTest`: host vazio, seis formatos inválidos (maiúscula,
underscore, hífen na ponta, ponto duplo, ponto no fim e 254 caracteres), host repetido marcado nos
dois campos, serviço desmarcado com host inválido e hosts válidos chegando ao provisionamento.
Faltam a verificação no browser e o aceite manual.

**24/09/2026** — Ajuste da revisão, que vai além do texto da issue: além do total de 253, cada
parte do host agora tem no máximo 63 caracteres, o limite de label do DNS. Sem isso, um host desses
passava pelo wizard e só era recusado pela API, aparecendo tarde como "Ingress failed".
