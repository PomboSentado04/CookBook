# CookBook — Android / Kotlin / Jetpack Compose

Aplicativo acadêmico de organização de receitas, preparado para Android 16 (API 36).

## Funcionalidades
- Cadastro, consulta, edição e exclusão de receitas (CRUD).
- Persistência local com Room em duas tabelas: `receitas` e `ingredientes`.
- Relacionamento 1:N com exclusão em cascata dos ingredientes.
- Pesquisa de receitas pela TheMealDB API usando Retrofit.
- Importação de receitas online para o banco local.
- Compartilhamento de receita (ingredientes + preparo) com outros apps via `Intent.ACTION_SEND`.

## Requisitos
- Android Studio atualizado, JDK 17.
- Android SDK Platform 36 instalado.
- Emulador configurado com Android 16 (API 36), ou aparelho com Android compatível.
- Internet no emulador/aparelho para consultar TheMealDB e carregar imagens.

## Como abrir e executar
1. Extraia o arquivo ZIP.
2. No Android Studio, escolha **Open** e selecione a pasta `CookBook`.
3. Aguarde o Gradle Sync e aceite a instalação dos componentes solicitados.
4. Se necessário, abra **Tools > SDK Manager** e instale **Android 16 / API 36**.
5. Crie/inicie um emulador com API 36 pelo Device Manager.
6. Clique em **Run**.

O projeto usa Gradle 8.13, Android Gradle Plugin 8.13.2, Kotlin 2.2.21 e Java 17.
O Android Studio baixa as dependências automaticamente na primeira sincronização.

## Como apresentar
1. Mostre a tela inicial e cadastre uma receita manualmente.
2. Abra a receita, edite algum campo e salve.
3. Compartilhe a receita para demonstrar o `ACTION_SEND`.
4. Exclua uma receita para demonstrar o CRUD.
5. Use a lupa para pesquisar um prato (ex.: `Arrabiata`) e importe o resultado.
6. Explique as entidades `Receita` e `Ingrediente`, o relacionamento e o DAO do Room.

## Estrutura
- `data/`: entidades Room, DAO e banco.
- `network/`: modelos Retrofit e interface TheMealDB.
- `ui/`: ViewModel e estado da aplicação.
- `MainActivity.kt`: interface Compose e navegação simples por estado.

## Observações
- A busca externa depende de conexão com a internet.
- Receitas importadas são gravadas no Room e continuam disponíveis offline.
- O aplicativo não precisa de chave de API para o endpoint público usado.
- O projeto contém fontes Kotlin e arquivos Gradle; não inclui APK pré-compilado.


## Correção da exclusão
A exclusão foi reforçada para localizar a receita diretamente pelo ID no Room (`DELETE FROM receitas WHERE id = :receitaId`), em vez de depender da comparação do objeto completo. A confirmação do diálogo também é reinicializada por ID de receita.


## Exclusão transacional reforçada
A exclusão remove explicitamente os ingredientes relacionados e, em seguida, a receita, dentro de uma transação Room (`excluirCompleta`). O ViewModel verifica o número de linhas removidas e apresenta uma mensagem na tela caso a operação falhe.
