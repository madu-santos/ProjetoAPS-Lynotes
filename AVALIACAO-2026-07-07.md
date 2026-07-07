# Avaliação — EQ09 (APS)

**Data:** 2026-07-07  
**Método:** verificação automática lendo `origin/main` + checagem do deploy ao vivo.

> Avaliação automática preliminar. O que não estiver no repositório e no ar é considerado não atendido.

---

## 1. Tecnologias obrigatórias

- ✅ **Docker** — Dockerfile na raiz + docker-compose
- ✅ **Javalin** — dependência no pom + uso no código
  - Evidência: `src/main/java/br/ufpb/dcx/projetos/App.java:10:import io.javalin.Javalin;`
- ✅ **Thymeleaf** — dependência no pom + templates com th:
  - Evidência: `src/main/resources/templates/admin/verificacoes.html:27:                <div th:if="${artistas.isEmpty()}" class="alert alert-info text-center" role="alert">`
- ✅ **Bootstrap** — referenciado nos templates/estáticos
  - Evidência: `src/main/resources/public/css/index.css:20:/* Sobrescreve cores primárias do Bootstrap */`
- ✅ **PostgreSQL** — driver no pom + uso confirmado
  - Evidência: `docker-compose.yaml:3:    image: postgres:16`

## 2. Estrutura esperada

- ✅ **Padrão Maven** — src/main/java + src/main/resources
- ✅ **Deploy em produção** — container ok; https://eq09.aps.rodrigor.com/ping → HTTP 200
- ✅ **Logs** — framework de log + uso no código
  - Evidência: `src/main/java/br/ufpb/dcx/projetos/App.java:31:    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);`
- ✅ **Testes automatizados** — 17 classe(s) de teste (JUnit no pom)
- ✅ **Autenticação** — hash de senha usado no código + login/sessão
  - Evidência: `src/main/java/br/ufpb/dcx/projetos/login/services/BCryptHashingStrategy.java:17:        return BCrypt.hashpw(senha, BCrypt.gensalt(12));`
- ✅ **Autorização** — papéis + verificação de acesso em rotas
  - Evidência: `src/main/java/br/ufpb/dcx/projetos/App.java:14:import io.javalin.security.RouteRole;`
- ✅ **Modularidade** — 8 camadas: controller, database, dto, exception, model, repository, service, view

## 3. Arquitetura e banco de dados

- **Classes por camada:** controller: 8, service: 11, repository: 13, model: 11, dto: 4, exception: 5, view: 5, database: 3 (total 60 classes)
- **Tabelas no banco:** 9 tabela(s): album, artista, comentario, musica, playlist, playlist_item, post, post_estrela, usuario
- **Entidades no código:** 11 entidade(s): Album, Artista, Comentario, Musica, Playlist, PlaylistItem, PlaylistWithItems, Post, Role, StatusVerificacao, Usuario

---

*Gerado automaticamente em 2026-07-07. Requisitos: tecnologias obrigatórias (Docker, Javalin, Thymeleaf, Bootstrap, PostgreSQL), estrutura esperada (padrão Maven, deploy, logs, testes, autenticação, autorização, modularidade), arquitetura em camadas e modelagem do banco.*