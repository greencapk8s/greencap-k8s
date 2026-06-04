# Transição do cluster para DISCONNECTED em timeout do fetch de namespaces

Quando o carregamento de namespaces no login falha por timeout, um cluster com `ConnectionStatus` igual a `CONNECTED` é transitado para `DISCONNECTED`.

## Por quê

Manter o status como `CONNECTED` após uma falha de conexão é informação incorreta — o dashboard exibiria um badge verde para um cluster inacessível. O `ConnectionStatus` é definido como um snapshot do que foi observado por último, logo uma falha no fetch de namespaces é uma observação válida.

## Trade-offs considerados

**Risco de falsos positivos**: uma falha de rede transitória (ex: queda de VPN) poderia marcar um cluster saudável como `DISCONNECTED`. Mitigado atualizando apenas quando o status atual é `CONNECTED` — clusters já em `DISCONNECTED`, `ERROR` ou `UNKNOWN` não são alterados por esse caminho. O usuário precisa acionar "Test Connection" explicitamente para restaurar `CONNECTED`.

**Somente `CONNECTED` → `DISCONNECTED`**: não usamos `ERROR` aqui porque `ERROR` significa que o cluster respondeu mas rejeitou a requisição (credenciais inválidas). Um timeout indica ausência de rota — o que mapeia para `DISCONNECTED` conforme o modelo de domínio.

**Sem monitoramento contínuo**: não existe job agendado que faça polling dos clusters. O `ConnectionStatus` é atualizado apenas por: (1) registro do cluster, (2) "Test Connection" explícito, e (3) este caminho de fetch de namespaces.
