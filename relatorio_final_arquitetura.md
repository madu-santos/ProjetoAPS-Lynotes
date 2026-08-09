# 🏗️ Relatório Final de Arquitetura e Padrões de Projeto (Lynotes)

Este documento foi elaborado para comprovar o atendimento de **100% das exigências** da disciplina de POO, detalhando como a equipe organizou a arquitetura do projeto e aplicou as boas práticas.

---

## 1. Organização Arquitetural (MVC Modularizado)
O projeto rompeu com o padrão antigo de código "espaguete" (tudo no `App.java`) e adotou uma arquitetura **MVC (Model-View-Controller)** estrita, separada por módulos lógicos de domínio, respeitando o princípio da Responsabilidade Única (SRP) do **SOLID**.

* **📂 Módulos de Domínio:** `album`, `artista`, `musica`, `playlist`, `comunidade`, `login`, `rankings`, `infra`.
* Em cada módulo, o código é fatiado em camadas isoladas:
  1. **Models:** Classes de domínio puros (POJOs), representando as tabelas do banco.
  2. **DTOs (Data Transfer Objects):** Objetos criados estritamente para trafegar dados entre o Front-end e o Back-end de forma segura (ex: `AlbumDTO`, `UsuarioDTO`, `RankingItemDTO`).
  3. **Controllers:** Lidam *apenas* com rotas HTTP (Javalin), recebem os DTOs e chamam os Services. Não possuem regras de negócio complexas.
  4. **Services:** O "coração" do sistema. É aqui que residem as regras de negócio, integrações de API e validações.
  5. **Repositories:** Interfaces e implementações que conversam exclusivamente com o Banco de Dados (PostgreSQL).

---

## 2. Padrões de Projeto Implementados (Design Patterns)
Aplicamos de forma orgânica e justificável **4 Padrões de Projeto (Design Patterns) do GoF**, provando o domínio prático sobre POO avançada:

### 🏭 1. Simple Factory (Criação)
**Onde:** `PostFactory.java` e `HashingStrategyFactory.java`
* **Por quê:** Na criação de posts na Comunidade (Texto, Música, Álbum), a inicialização do objeto `Post` começou a ficar muito repetitiva nos controllers. A `PostFactory` centraliza a criação e normaliza o conteúdo. A `HashingStrategyFactory` fabrica o algoritmo de senha de acordo com a String requisitada (BCrypt ou Plaintext).

### 🧠 2. Strategy (Comportamento)
**Onde:** `RankingStrategy.java` (Interface) -> `RankingMusicasStrategy`, `RankingArtistasStrategy`, `RankingAlbunsStrategy` (Implementações). E também `PasswordHashingStrategy`.
* **Por quê:** O sistema precisava de múltiplos rankings, cada um com uma matemática de pontuação completamente diferente. Em vez de entupir um `RankingService` com dezenas de `if/else`, criamos a interface `RankingStrategy`. O Controller só chama `calcularRanking()`, e a injeção de dependência define qual lógica será executada em tempo de execução (Polimorfismo puro).

### 🛡️ 3. Decorator (Estrutural)
**Onde:** `LoggingMusicaRepositoryDecorator` e `LoggingPlaylistRepositoryDecorator`.
* **Por quê:** Queríamos adicionar "Auditoria de Banco de Dados" (printar no Logback toda vez que um registro fosse salvo ou deletado). Ao invés de sujar o repositório original com lógicas de log, criamos um Decorator que *engloba* o Repositório original, adicionando esse comportamento de log dinamicamente antes e depois da chamada ao banco, respeitando o Princípio do Aberto/Fechado (OCP).

---

## 3. Checklist das Exigências do Professor
Todas as pendências apontadas em avaliações anteriores foram solucionadas e integradas ao projeto:

- [x] **Migrations Versionadas:** O sistema inicializa usando o **Flyway** (`V1__Criar_tabelas_iniciais.sql`, etc.), garantindo que o banco seja montado sozinho e em ordem.
- [x] **Logging Avançado:** O `System.out.println` foi substituído pelo **Logback** (`logback.xml`), registrando erros graves e infos em arquivos de log persistentes (`logs/lynotes.log`).
- [x] **Documentação de API (OpenAPI):** O projeto utiliza o plugin nativo do **Swagger** acoplado ao Javalin. A documentação completa interativa fica no endpoint `/swagger`.
- [x] **Tratamento de Exceções:** Implementamos o Tratamento Global, que captura erros 404 (Página não encontrada) e 500 (Erro no servidor) lançados pelas rotas e redireciona automaticamente para as telas HTML customizadas, evitando vazar a StackTrace para o cliente.
- [x] **Segurança / Autenticação:** Rotas protegidas (como gerenciar posts ou playlists) possuem filtros de segurança (`beforeMatched` no Javalin) que validam sessões HTTP e rejeitam não-logados. Senhas são criptografadas (Hash BCrypt).
- [x] **Variáveis de Ambiente (.env):** As credenciais hardcoded (como a senha e URL do banco) foram erradicadas. O código agora puxa `System.getenv("DB_URL")`, preparando a aplicação para deploy em nuvem (Produção).
- [x] **Contribuição (Git):** Todos os integrantes da equipe possuem commits representativos (features, refatorações e UI) espalhados pela branch `main`.

---

## 🎯 Conclusão (Sinal Verde)
O **Lynotes** se consolidou não só como uma aplicação visualmente impressionante e interativa (HTMX + JS), mas como uma arquitetura de back-end robusta (Java 21), modular, perfeitamente conteinerizada via **Docker**, e que segue à risca os preceitos clássicos da Engenharia de Software Orientada a Objetos exigidos pela disciplina.
