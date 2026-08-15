# 02 — Preferência de Tour visto no usuário

Status: todo

Pré-requisito das issues 03 e 05. Independente da issue 01.

O Tour precisa aparecer uma vez e não voltar a cada login. Alguma coisa tem que lembrar que aquele usuário já passou por ele, e essa memória fica no banco, junto do usuário — não no navegador. É o mesmo raciocínio que já vale para o tema, a largura do drawer e o intervalo de atualização: trocar de máquina ou limpar o cache não deveria ressuscitar a tela de boas-vindas.

A entidade `User` ganha uma quarta preferência, no formato exato das três que já existem: uma coluna booleana `tour_seen` na tabela `users`, criada por uma migration Flyway nova, com valor padrão falso para que todo usuário já cadastrado — inclusive o `admin` que o `DataInitializer` cria — entre na primeira execução ainda sem ter visto o Tour. O `UserService` ganha o par de métodos de leitura e escrita que acompanha cada preferência ali, seguindo a mesma nomenclatura e o mesmo tratamento transacional dos vizinhos.

O nome do campo em Java precisa render um leitor que soe como afirmação, conforme a convenção de nomenclatura do projeto, sem cair no que o Lombok geraria a partir de um nome já prefixado com verbo auxiliar — o resultado seria um método com o prefixo duplicado.

Quem grava é a conclusão do Tour, em qualquer das saídas que a Sprint decidiu equivaler: chegar ao último passo, pular, apertar ESC ou fechar no X. Todas marcam como visto. A alternativa — deixar o ESC de fora — significaria receber o mesmo tour amanhã por causa de uma tecla apertada sem querer, e a reabertura pela tela de Settings (issue 05) já atende quem fechou e se arrependeu.

Quem lê é o `MainLayout`, na entrada da sessão, e a leitura sozinha não decide nada: o Tour só dispara se, além de não ter sido visto, o usuário tiver um Cluster ativo. Essa segunda condição existe por causa do caminho de instalação via `docker compose`, onde nenhum Cluster é registrado e os itens de menu ficam inertes — disparar ali seria apresentar uma tela que não responde. Com a condição, esse usuário recebe o Tour no primeiro login depois de registrar um Cluster, que é exatamente quando ele passa a fazer sentido.

Cobertura de teste: `PostgresIntegrationTest` cobre a persistência — o valor nasce falso, sobrevive a uma releitura depois de gravado, e é isolado por usuário, de modo que marcar um como visto não afeta outro.

Fora de escopo: qualquer interface para essa preferência. O card em Platform Settings é a issue 05, e o consumo pelo gatilho é a issue 03.
