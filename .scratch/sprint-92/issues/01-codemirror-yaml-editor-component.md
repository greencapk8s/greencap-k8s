---
title: "CodeMirror YAML Editor — componente Vaadin reutilizável"
status: done
priority: high
sprint: 92
---

Criar um componente Vaadin customizado que encapsula o CodeMirror 6 como editor de código YAML, seguindo o mesmo padrão do `TopologyGraphComponent` (Lit + `@NpmPackage`).

O componente deve suportar syntax highlighting para YAML, números de linha, indentação automática de 2 espaços e fonte monoespaçada. O tema (claro/escuro) deve sincronizar automaticamente com o atributo Lumo do documento. A altura é configurável por instância.

A API Java deve expor `getValue()`, `setValue(String)`, `setReadOnly(boolean)`, `focus()`, `setHeight(String)` e `setVisible(boolean)` — o suficiente para substituir os `TextArea` existentes sem alterar a lógica das views. O valor deve ser sincronizado servidor-cliente via propriedade com evento `value-changed`, garantindo que `getValue()` retorne o conteúdo atual no momento do submit.
