# 04 — Deploy Template: preview, build via Kaniko e apply genérico

Status: done

Depende das issues 02 e 03.

Ação de deploy acionada a partir de um card na Templates Catalog. Abre um diálogo com o preview somente-leitura dos manifests concatenados do Template (sem edição — Templates são curados, não autorados pelo usuário) e um botão de confirmação.

Na confirmação: (1) aplica o arquivo de recurso que contém a Namespace do Template — não a Namespace ativa da sessão; (2) para cada entrada em `builds` do `template.yaml`, dispara um Kaniko Build reaproveitando o mecanismo já existente em Deploy from Dockerfile/Import Compose, usando o próprio `greencap-templates` como Git Repository de contexto e pushando para o Registry interno do Cluster (nunca um registro externo); (3) substitui, no arquivo de recurso correspondente, o valor-sentinela `__BUILD__<name>` pela referência da imagem recém-publicada; (4) aplica os demais arquivos de recurso, em ordem, via client genérico do Fabric8 (`GenericKubernetesResource`/dynamic client, já que os tipos não são conhecidos de antemão como no restante do GreenCap).

Em caso de conflito (algum recurso — tipicamente a Namespace, de um deploy anterior do mesmo Template — já existe), aborta no primeiro erro sem desfazer o que já foi aplicado, mesmo padrão de "no rollback on failure" de Import Compose e Deploy from Dockerfile. Erros de build seguem o mesmo padrão: log inline de progresso do Kaniko (reaproveitando o polling já usado em `BuildLogsView`/`DeployFromDockerfileView` via `AsyncTasks.schedulePolling`), sem prosseguir para o apply dos recursos restantes se o build falhar.

Ao final (sucesso completo ou parcial), a Templates Catalog atualiza o card para "Installed" assim que a Namespace do Template existir no Cluster.
