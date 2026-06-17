---
title: Integração com Build Kaniko para serviços com `build:`
status: open
sprint: 83
---

Serviços do Compose que declaram `build:` precisam ter sua imagem construída antes da criação dos recursos Kubernetes. O GreenCap reutiliza o mecanismo de Build Kaniko existente (ADR 0007): um Job in-cluster no namespace `greencap-system`, com contexto Git apontando para o mesmo repositório fornecido no passo 1 do wizard, e `--dockerfile` derivado do campo `context`/`dockerfile` do Compose.

O nome da imagem produzida usa `image:` do Compose quando declarado junto de `build:`; quando ausente, usa `<service-name>:latest` como default — valor editável pelo usuário na tela de revisão antes de iniciar os builds.

Quando há múltiplos serviços com `build:`, os builds são executados em sequência. Cada build exibe seu log em tempo real na tela de execução (passo 3 do wizard), seguindo o padrão de `BuildView`. Se um build falhar, os builds subsequentes ainda são tentados; o resultado acumula sucesso e falha por serviço.

Um serviço cujo build falhou não tem seus recursos Kubernetes criados — o Deployment não é provisionado sem a imagem.

A integração deve reutilizar a infraestrutura existente de `BuildService` / `BuildJobFactory` sem duplicar lógica de criação de Job Kaniko.
