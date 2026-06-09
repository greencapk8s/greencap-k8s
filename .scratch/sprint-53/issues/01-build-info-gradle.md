---
id: 01
title: Expor versão do build via BuildProperties
status: done
---

## O que fazer

- Habilitar o task `bootBuildInfo` no `build.gradle.kts` vinculado ao `processResources`, para que `META-INF/build-info.properties` seja gerado tanto no `bootRun` quanto no `bootJar`
- Confirmar que `BuildProperties` é injetável como bean Spring

## Critério de aceite

`BuildProperties.getVersion()` retorna o valor de `version` definido no `build.gradle.kts` ao rodar a aplicação.
