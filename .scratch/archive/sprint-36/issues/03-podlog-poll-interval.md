# 03 — PodLog: intervalo de poll configurável

**Status:** done

## Contexto
O intervalo de poll do PodLog está hardcoded como 3 segundos (`POLL_INTERVAL_SECONDS = 3`). O objetivo é torná-lo selecionável na toolbar da própria página, por sessão, sem persistência.

## Entrega
- `PodLogsView`: `Select<Integer>` com opções [1, 3, 5, 10] (segundos), valor padrão 3
- Label "Poll:" ao lado do select, antes do botão Pause/Resume
- Ao mudar o intervalo: se polling ativo, reinicia com novo intervalo; se pausado, apenas atualiza o valor
- Constante `POLL_INTERVAL_SECONDS` substituída pelo valor do select

## Restrições
- Sem persistência — preferência é por sessão/contexto
- Comportamento de Pause/Resume não muda
