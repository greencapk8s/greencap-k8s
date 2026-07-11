# 02 — Modo não-interativo via variáveis de ambiente em setup.sh e teardown.sh

Status: in-progress

`setup.sh` faz três prompts interativos (`read -rp`): confirmar o auto-install de dependências faltantes, escolher o profile de recursos (Minimal/Recommended/Custom) e, se Custom, pedir nodes/CPUs/RAM. `teardown.sh` faz um quarto prompt, pedindo que o usuário digite "yes" para confirmar a exclusão do cluster. Nenhum dos dois pode rodar sem intervenção humana hoje, o que impede automação (CI ou qualquer script que chame o setup de ponta a ponta).

O script já tem um precedente para isso: `GREENCAP_ENCRYPTION_KEY` e `DB_PASSWORD` são lidos do ambiente quando já estão setados, pulando a geração/prompt correspondente. Esta issue estende o mesmo padrão aos demais prompts — cada `read -rp` passa a checar primeiro se a variável de ambiente correspondente já está definida (`AUTO_INSTALL`, `PROFILE_CHOICE`, e quando `PROFILE_CHOICE=3`, `NODES`/`CPUS`/`MEMORY`) e só pergunta interativamente se ela estiver ausente. Em `teardown.sh`, a variável `CONFIRM` segue a mesma lógica, substituindo a digitação manual de "yes".

O comportamento interativo atual (sem nenhuma variável setada) não muda — quem já usa o script normalmente não percebe diferença. O benefício vale tanto para automação externa ao script (CI, outros scripts de provisionamento) quanto para um usuário avançado que queira rodar tudo com um único comando sem prompts.
