# Karibu-Testing para testes de views Vaadin

GreenCap tem ~35 views Vaadin com lógica de UI que não é verificada por testes automatizados: guards de permissão, estado de dialogs destrutivos, validação de confirm fields. Escolhemos Karibu-Testing (in-memory, sem browser) em vez de Selenium/Playwright para cobrir essa camada.

Karibu executa em milissegundos por rodar o ambiente Vaadin em memória — sem browser, sem Docker adicional, sem flakiness de timing. A contra-partida é que não testa renderização visual, CSS ou comportamento real do browser. Para GreenCap essa troca é aceitável: o risco está na lógica de orquestração da view (botão habilitado/desabilitado, dialog abre ou não, serviço chamado ou não), não na fidelidade visual. Testes E2E com browser real são o complemento natural quando a cobertura visual se tornar necessária.

## Considered Options

**Selenium/Playwright** — testa o browser real, cobre CSS e interações complexas, mas é lento (~segundos por teste), sujeito a flakiness e requer infraestrutura adicional. Não justificado para verificar lógica de estado de componente.

**Sem testes de view** — aceitável enquanto há poucas views; inviável com 35+ views e operações destrutivas irreversíveis no domínio (Delete Namespace apaga todos os recursos dentro do namespace).
