---
id: "02"
title: "Deploy Application — Wizard UI: DeployApplicationView, sidebar, CTA na Dashboard"
status: done
labels: [feature]
sprint: 75
---

## Contexto

Depende da issue 01 (backend + permissão). Esta issue cobre toda a camada de UI: wizard multi-step como View dedicada, item no menu lateral (seção PROJECT) e CTA na DashboardView quando o namespace está sem Workloads.

Decisões de design (ver CONTEXT.md → **Deploy Application** e ADR 0009):
- View dedicada com rota `deploy`, layout `MainLayout` — não é Dialog (5+ passos, muito para modal).
- 6 passos: (1) Name, (2) Image & Port, (3) Replicas & Resources, (4) Volume, (5) External Access, (6) Review & Deploy.
- Após sucesso → navega para `TopologiaView` do novo Namespace.
- Em falha parcial → notificação de erro listando o que foi criado e o que falhou; não navega.
- Protegida por `PROJECT_DEPLOY_APPLICATION` (issue 01).

## Entrega

### 1. `ui/DeployApplicationView.java` (novo)

```java
@Route(value = "deploy", layout = MainLayout.class)
@PageTitle("Deploy Application — GreenCap K8s")
@RolesAllowed("PROJECT_DEPLOY_APPLICATION") // via @PermitAll + SecurityUtils check no construtor
```

A View é uma `VerticalLayout` com três partes:
1. **Step indicator** (topo): `HorizontalLayout` com 6 `Span` numerados, o passo atual destacado com tema `badge primary`.
2. **Content area**: `Div` que troca de conteúdo a cada passo (sem animação).
3. **Footer**: `HorizontalLayout` com botão "Back" (oculto no passo 1), "Next" (passos 1–5), "Deploy" (passo 6).

#### Passos

**Passo 1 — Application Name**
`FormLayout` com `ResponsiveStep("0", 1)`:
- `TextField` "Application name" — obrigatório, validação: regex `[a-z0-9][a-z0-9\-]{0,61}[a-z0-9]?` (k8s namespace rules), max 63 chars. Mensagem de erro: "Use lowercase letters, numbers and hyphens only (max 63 chars)".
- `Span` informativo: "This will also be the Kubernetes Namespace name."

**Passo 2 — Container Image & Port**
`FormLayout` com `ResponsiveStep("0", 1)`:
- `ComboBox<String>` "Container image" — campo livre (`setAllowCustomValue(true)`). Items carregados async: combinação de `registry.kube-system.svc.cluster.local:80/<repo>:<tag>` para cada Tag de cada Repository do Registry interno (via `RegistryService.listRepositories` + `listTags`). Se Registry inacessível, items vazio sem erro (campo livre ainda funciona). Placeholder: `nginx:latest`.
- `IntegerField` "Container port" — opcional, sem valor padrão. Helper: "Port your application listens on. Required for Service and Ingress creation."

**Passo 3 — Replicas & Resources**
`FormLayout` com `ResponsiveStep("0", 2)`:
- `IntegerField` "Replicas" — obrigatório, min 1, default 1.
- Linha em branco (espaçador para manter grid 2 colunas).
- `TextField` "CPU request" — default "100m". `TextField` "CPU limit" — default "500m".
- `TextField` "Memory request" — default "128Mi". `TextField` "Memory limit" — default "512Mi".

**Passo 4 — Persistent Volume**
`FormLayout` com `ResponsiveStep("0", 1)`:
- `Checkbox` "Add persistent storage" — default false. Ao marcar, exibe os campos abaixo.
- `ComboBox<String>` "Storage class" — items carregados via `StorageService.listStorageClasses`; pré-selecionar o StorageClass com `metadata.annotations["storageclass.kubernetes.io/is-default-class"] = "true"`, se existir.
- `IntegerField` "Size (Gi)" — min 1, default 1.
- `TextField` "Mount path" — default "/data".

**Passo 5 — External Access**
`FormLayout` com `ResponsiveStep("0", 1)`:
- `Checkbox` "Expose application externally (Ingress)" — desabilitado (com tooltip) se containerPort não foi informado no passo 2. Default false.
- `TextField` "Host" — pré-preenchido com `<namespace>.greencap.local` (namespace do passo 1). Editável.
- `ComboBox<String>` "Ingress class" — items carregados via `NetworkingService.listIngressClassNames`.
- Campos de host e ingress class visíveis apenas quando checkbox ativo.

**Passo 6 — Review & Deploy**
`VerticalLayout` com um `Div` por recurso que será criado:
- Sempre: Namespace `<name>`, Deployment `<name>` (imagem, réplicas, CPU/mem).
- Se porta informada: Service `<name>` (ClusterIP, porta `<port>`).
- Se volume: PVC `<name>-pvc` (storage class, tamanho, mount path).
- Se ingress: Ingress `<name>-ingress` (host, ingress class).

Cada item como `HorizontalLayout` com ícone `VaadinIcon.CHECK_CIRCLE` (cor success) + texto descritivo.

#### Lógica de navegação e submissão

```java
private void onDeploy() {
    DeployApplicationRequest request = buildRequest();
    Cluster cluster = clusterContext.getCluster();
    setDeployButtonEnabled(false);
    UI ui = UI.getCurrent();
    Thread.ofVirtual().start(() -> {
        try {
            DeployApplicationResult result = deployApplicationService.deploy(cluster, request);
            ui.access(() -> {
                if (result.isFullSuccess()) {
                    clusterContext.setNamespace(request.namespace());
                    userService.updateActiveNamespace(currentUsername(), request.namespace());
                    ui.navigate(TopologiaView.class);
                } else {
                    String created = String.join(", ", result.createdResources());
                    showError("Partial failure at " + result.failedStep() + ": " + result.failureMessage()
                            + ". Created: " + created);
                    setDeployButtonEnabled(true);
                }
            });
        } catch (KubernetesOperationException e) {
            ui.access(() -> {
                showError(e.getMessage());
                setDeployButtonEnabled(true);
            });
        }
    });
}
```

- `showError` → `Notification` em `BOTTOM_END` com `NotificationVariant.LUMO_ERROR`.
- `currentUsername()` → `SecurityContextHolder.getContext().getAuthentication().getName()`.

### 2. `ui/MainLayout.java`

Em `buildVisaoGeralNav()`, adicionar item "Deploy Application" antes de `topologia`:

```java
boolean canDeploy = SecurityUtils.hasPermission(Permission.PROJECT_DEPLOY_APPLICATION);
SideNavItem deployApp = navItem("Deploy Application", DeployApplicationView.class, VaadinIcon.ROCKET, canDeploy);
```

Adicionar `deployApp` em `addIfEnabled(deployApp, topologia, ...)` e em `nav.addItem(deployApp, topologia, ...)`.

### 3. `ui/DashboardView.java`

CTA quando o namespace não tem Deployments (estado vazio). Após carregar as estatísticas do dashboard, verificar se `deploymentCount == 0`. Se sim, adicionar antes do grid de cards um `Div` de destaque:

```java
private Div buildDeployApplicationCta() {
    Span title = new Span("This namespace is empty");
    title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.FontSize.LARGE);

    Span subtitle = new Span("Deploy your first application from a container image.");
    subtitle.addClassNames(LumoUtility.TextColor.SECONDARY);

    Button deployButton = new Button("Deploy Application", VaadinIcon.ROCKET.create(),
            e -> UI.getCurrent().navigate(DeployApplicationView.class));
    deployButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

    VerticalLayout ctaContent = new VerticalLayout(title, subtitle, deployButton);
    ctaContent.setAlignItems(FlexComponent.Alignment.CENTER);
    ctaContent.setPadding(true);

    Div cta = new Div(ctaContent);
    cta.getStyle()
            .set("border", "2px dashed var(--lumo-contrast-20pct)")
            .set("border-radius", "var(--lumo-border-radius-l)")
            .set("padding", "var(--lumo-space-xl)")
            .set("text-align", "center")
            .set("width", "100%");
    return cta;
}
```

CTA visível apenas se `SecurityUtils.hasPermission(Permission.PROJECT_DEPLOY_APPLICATION)` — usuários sem a permissão veem o dashboard vazio normalmente.

## Critérios de aceite

- `./gradlew compileJava` passa.
- `./gradlew test` passa.
- Aceite manual no `greencap-demo`:
  1. Usuário ADMIN/OPERATOR vê "Deploy Application" na sidebar seção PROJECT.
  2. Wizard abre em `/deploy`, navega pelos 6 passos com Back/Next funcionando corretamente.
  3. Passo 2: sugestões do Registry interno aparecem no ComboBox (se Registry disponível).
  4. Passo 4: StorageClasses do cluster disponíveis em dropdown; padrão pré-selecionado.
  5. Passo 5: desabilitado se passo 2 sem porta; IngressClasses do cluster disponíveis.
  6. Passo 6: review lista corretamente apenas os recursos que serão criados.
  7. "Deploy" cria todos os recursos, navega para Topologia do novo Namespace mostrando o grafo.
  8. Dashboard vazia (namespace sem Deployments): CTA "Deploy Application" visível e navega para o wizard.
  9. Usuário VIEWER não vê "Deploy Application" na sidebar e não acessa `/deploy` diretamente.
