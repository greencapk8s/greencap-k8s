---
title: TestContainers — substituir H2+Flyway-desabilitado por PostgreSQL real no profile de teste
status: done
sprint: 81
---

## Problem

O profile de teste atual (`application-test.yaml`) usa H2 em modo PostgreSQL com `flyway.enabled: false` e `ddl-auto: create-drop`. Isso significa que as 27 migrations Flyway nunca são executadas em nenhum teste — uma migration com SQL PostgreSQL-específico quebrado só aparece em produção.

## Expected

### 1 — Dependências TestContainers no `build.gradle.kts`

Adicionar:
```kotlin
testImplementation("org.springframework.boot:spring-boot-testcontainers")
testImplementation("org.testcontainers:postgresql")
testImplementation("org.testcontainers:junit-jupiter")
```

Remover:
```kotlin
testRuntimeOnly("com.h2database:h2")
```

### 2 — `application-test.yaml` atualizado

Remover toda a configuração de datasource/H2/Flyway manual. O `@ServiceConnection` do Spring Boot 3 auto-configura o datasource a partir do container. Manter apenas a chave de encriptação:

```yaml
greencap:
  encryption:
    key: test-encryption-key-exactly-32ch
```

### 3 — `GreenCapApplicationTests` reescrito

```java
@SpringBootTest
@Testcontainers
class GreenCapApplicationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Test
    void migrationsRunClean() {
        // context load com Flyway ativo já valida que todas as migrations rodam sem erro
    }
}
```

O teste valida implicitamente que:
- Todas as 27 migrations executam em ordem sem erro no PostgreSQL real
- O `DataInitializer` cria o usuário admin sem exceção
- O contexto Spring Boot carrega completamente
