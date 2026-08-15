# 05 — Card de Onboarding em Platform Settings

Status: todo

Consome as issues 02 e 04.

Quem pulou o Tour, fechou por engano ou simplesmente quer rever precisa de um caminho de volta. Sem ele, a preferência da issue 02 é uma porta que só fecha.

A `PlatformSettingsView` ganha um terceiro card, ao lado dos de Refresh e Appearance, com o mesmo formato dos vizinhos: título, uma linha de descrição e o controle. O controle é um botão que reinicia o Tour.

A tela de Settings foi escolhida em lugar de um menu de usuário no header porque esse menu não existe — a navbar tem o nome do usuário como texto simples e um botão de logout solto. Criá-lo significaria uma superfície nova e, quase certamente, mover o logout para dentro dela: uma mexida em UX existente que não pertence a esta sprint. Settings, além de já existir, é onde as outras três preferências do usuário são editadas, então o Tour entra no lugar que o usuário já aprendeu a procurar.

O botão navega para o Dashboard e inicia o Tour imediatamente. Não altera a preferência: ela registra que o usuário já foi apresentado ao produto, o que continua verdade depois de rever. Gravar falso e esperar o próximo login faria o clique parecer que não funcionou. E disparar sem sair da tela de Settings funcionaria para os quatro passos de header e menu, que existem em qualquer view, mas deixaria o passo de fecho — apontando que o que está na tela é o próprio GreenCap rodando — sem sentido algum.

Cobertura de teste: Karibu estendendo `KaribuTest` — o card aparece na view, e o botão leva ao Dashboard. Que o Tour de fato abra é verificação de navegador, coberta pelo aceite manual junto com a issue 04.

Fora de escopo: qualquer opção de desativar o Tour permanentemente, ou de escolher quais passos ver. São seis passos numa única execução; controle mais fino seria configuração para um problema que ninguém relatou.
