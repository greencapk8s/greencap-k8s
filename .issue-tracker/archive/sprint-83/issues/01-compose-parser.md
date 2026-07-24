---
title: Parser de docker-compose.yml a partir de Git Repository
status: done
sprint: 83
---

O GreenCap precisa buscar e interpretar um `docker-compose.yml` a partir de uma URL de Git Repository público. O resultado do parse deve ser um modelo intermediário que represente os serviços, portas, variáveis de ambiente, volumes e instruções de build encontrados no arquivo, sem nenhum acoplamento à UI ou à camada Kubernetes.

O parser recebe a URL do repositório Git, o branch e o path opcional para o arquivo dentro do repositório (padrão: `docker-compose.yml` na raiz). O clone é feito de forma efêmera, apenas para leitura do arquivo.

A tradução de `environment:` segue a heurística de chave sensível: chaves contendo `PASSWORD`, `SECRET`, `TOKEN`, `KEY` ou `CREDENTIAL` (case-insensitive) são classificadas como sensíveis; as demais como não sensíveis.

Volumes do tipo named (sem prefixo `./` ou `/`) são classificados como candidatos a PVC. Volumes do tipo bind-mount são classificados como ignorados.

Diretivas não reconhecidas ou não suportadas (como `networks:`, `restart:`, `healthcheck:`, `deploy:`, `profiles:`, `logging:`, `ulimits:`) são coletadas em uma lista separada para exibição de aviso na tela de revisão.

O modelo intermediário resultante deve ser suficientemente completo para que a camada de revisão da UI possa renderizá-lo e permitir edição dos campos configuráveis (tamanho de PVC, StorageClass, nome de imagem para serviços com `build:`) antes de qualquer chamada ao cluster.
