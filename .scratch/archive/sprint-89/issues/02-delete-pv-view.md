---
id: "89-02"
title: "PersistentVolumesView — ação Delete com guard de Bound"
status: done
priority: high
sprint: 89
---

`PersistentVolumesView` recebe a ação "Delete" como `SelectionAction.destructive` no section header, visível apenas para usuários com `GLOBAL_INFRASTRUCTURE_PV_DELETE`.

O botão fica desabilitado quando o PV selecionado está com status `Bound` — independentemente da permissão do usuário. O tooltip do botão indica "Cannot delete a Bound PV — delete the PVC first" quando desabilitado por status.

Ao confirmar, um dialog simples exibe o nome do PV e um aviso de que a operação é irreversível. Botão "Delete" (variante error) confirma; "Cancel" cancela. Após deleção bem-sucedida, a grid é recarregada.

A lógica de habilitação do botão deve reagir tanto à seleção da linha quanto ao status do PV selecionado.
