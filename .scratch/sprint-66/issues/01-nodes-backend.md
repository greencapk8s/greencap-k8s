---
id: "01"
title: "Workloads — backend: PodNodeResolver e coluna Nodes em DeploymentInfo/ReplicaSetInfo/StatefulSetInfo/JobInfo"
status: done
labels: [feat, backend]
sprint: 66
---

## Contexto

O `greencap-demo` agora roda com 3 Nodes (sprint 65 — driver `docker`, control-plane + 2 workers). `PodInfo` já expõe `node` (via `pod.getSpec().getNodeName()`), exibido em `PodsView`. Os demais tipos de Workload controllers (`Deployment`, `ReplicaSet`, `StatefulSet`, `Job`) não expõem em qual(is) Node(s) seus Pods estão rodando — informação relevante agora que os Pods de um mesmo Workload podem estar espalhados entre Nodes.

Decisões definidas via `/grill-with-docs` (ver `CONTEXT.md` — termos `Deployment`, `ReplicaSet`, `StatefulSet`, `Job`, já atualizados):

- Escopo: `DeploymentInfo`, `ReplicaSetInfo`, `StatefulSetInfo`, `JobInfo` ganham um novo campo `nodes` (String). `CronJobInfo` fica de fora — CronJob não possui Pods próprios.
- Formato: lista de nomes de Node distintos, separados por vírgula (ex: `"greencap-demo, greencap-demo-m02"`), mesmo padrão de colapso usado em `IngressInfo.hosts`. Vazio (nenhum Pod casado) → `"—"`, mesma convenção de `ReplicaSetInfo.owner` / `StatefulSetInfo.serviceName`.
- Estratégia de matching: dentro do mesmo `try-with-resources` de cada `list*`, buscar todos os Pods do namespace (ou `inAnyNamespace()` quando `isAllNamespaces`) **uma única vez**, e casar `spec.selector.matchLabels` do Workload contra `pod.metadata.labels` (mesmo modelo do `Service` → Pod descrito no `CONTEXT.md`). Para `Job`, `spec.selector.matchLabels` é gerado automaticamente pelo controller (`controller-uid`) e os Pods do Job carregam o mesmo label — não precisa de tratamento especial.
- Organização: `WorkloadService` já está em 455 linhas (limite do guia: ~200). A lógica de match + formatação é extraída para uma classe utilitária stateless nova, `PodNodeResolver`, reaproveitada pelos 4 métodos.

## Entrega

### 1. `PodNodeResolver` (novo)

Novo arquivo `kubernetes/PodNodeResolver.java`, classe final stateless com construtor privado:

```java
package io.greencap.k8s.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;

import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

public final class PodNodeResolver {

    private static final String NO_NODES = "—";

    private PodNodeResolver() {
    }

    public static String resolveNodes(List<Pod> pods, String namespace, Map<String, String> matchLabels) {
        if (matchLabels == null || matchLabels.isEmpty()) {
            return NO_NODES;
        }

        var nodes = new TreeSet<String>();
        for (Pod pod : pods) {
            if (!namespace.equals(pod.getMetadata().getNamespace())) continue;
            if (!matchesLabels(pod, matchLabels)) continue;
            Optional.ofNullable(pod.getSpec())
                    .map(spec -> spec.getNodeName())
                    .ifPresent(nodes::add);
        }

        return nodes.isEmpty() ? NO_NODES : String.join(", ", nodes);
    }

    private static boolean matchesLabels(Pod pod, Map<String, String> matchLabels) {
        Map<String, String> podLabels = Optional.ofNullable(pod.getMetadata().getLabels()).orElse(Map.of());
        return matchLabels.entrySet().stream()
                .allMatch(entry -> entry.getValue().equals(podLabels.get(entry.getKey())));
    }
}
```

`TreeSet` garante ordem alfabética estável dos nomes de Node (relevante para o filtro de texto e para snapshots de teste).

### 2. DTOs — novo campo `nodes`

Inserido imediatamente antes de `age` em cada record (posição refletida pela coluna "Nodes" na grid, sempre antes de "Age"):

- `DeploymentInfo(String name, String namespace, int desired, int ready, int available, String nodes, String age)`
- `ReplicaSetInfo(String name, String namespace, String owner, int desired, int ready, String nodes, String age)`
- `StatefulSetInfo(String name, String namespace, int desired, int ready, int available, String serviceName, String nodes, String age)`
- `JobInfo(String name, String namespace, String status, String completions, String duration, String nodes, String age, String owner)`

### 3. `WorkloadService` — `listDeployments`, `listReplicaSets`, `listStatefulSets`, `listJobs`

Em cada um dos 4 métodos, dentro do `try-with-resources` já existente:

1. Buscar todos os Pods do escopo (mesma lógica de `isAllNamespaces` já usada para os próprios itens):
   ```java
   var pods = isAllNamespaces(namespace)
           ? client.pods().inAnyNamespace().list().getItems()
           : client.pods().inNamespace(namespace).list().getItems();
   ```
2. No `.map(...)`, calcular `nodes` via:
   ```java
   PodNodeResolver.resolveNodes(pods, d.getMetadata().getNamespace(), selectorMatchLabels(
           Optional.ofNullable(d.getSpec()).map(s -> s.getSelector()).orElse(null)))
   ```
3. Novo helper privado estático, único, reaproveitado pelos 4 métodos:
   ```java
   private static Map<String, String> selectorMatchLabels(LabelSelector selector) {
       if (selector == null || selector.getMatchLabels() == null) return Map.of();
       return selector.getMatchLabels();
   }
   ```
   (`import io.fabric8.kubernetes.api.model.LabelSelector;` e `import io.fabric8.kubernetes.api.model.Pod;`)

Aplicar nos 4 métodos:
- `listDeployments`: `d.getSpec().getSelector()` (`DeploymentSpec`)
- `listReplicaSets`: `rs.getSpec().getSelector()` (`ReplicaSetSpec`)
- `listStatefulSets`: `sts.getSpec().getSelector()` (`StatefulSetSpec`)
- `listJobs`: `job.getSpec().getSelector()` (`JobSpec`)

`listPods` **não muda** — já expõe `node` (singular) por Pod.

## Critérios de aceite

- `./gradlew compileJava` e `./gradlew test` sem erros.
- Em um cluster com 3 Nodes (`samples/greencap-demo`), `listDeployments`/`listReplicaSets`/`listStatefulSets`/`listJobs` retornam `nodes` com os nomes corretos dos Nodes onde os respectivos Pods estão (validável comparando com `kubectl get pods -o wide`).
- Workload com 0 Pods correspondentes (ex: ReplicaSet antigo com `desired=0`) retorna `nodes = "—"`.
- Deployment/StatefulSet com réplicas distribuídas em 2+ Nodes retorna a lista comma-separated, ordenada alfabeticamente, sem duplicatas.
- Nenhuma regressão nos campos existentes de `DeploymentInfo`/`ReplicaSetInfo`/`StatefulSetInfo`/`JobInfo` (ordem dos demais campos preservada, apenas `nodes` inserido antes de `age`).

## Comments
