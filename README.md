# Lynotes - Painel Administrativo (APS/POO)

Este repositório contém a entrega inicial do **Lynotes**, um painel administrativo web para gerenciamento e catalogação de músicas, artistas e álbuns. O foco desta etapa ("Walking Skeleton") foi estabelecer a arquitetura do projeto e mitigar os riscos de infraestrutura técnica.

## Tecnologias

- Java 21
- Javalin 6 com Jetty
- Thymeleaf 3 e Bootstrap 5
- PostgreSQL 16
- JDBC
- Maven
- Docker e Docker Compose
- JUnit 5

## Execução com Docker

### Pré-requisitos

- Docker Desktop instalado e em execução.
- Portas `8080` e `5432` disponíveis.
- Maven instalado apenas caso os testes sejam executados fora do Docker.

### Iniciar aplicação e banco

Na raiz do projeto, execute:

```bash
docker compose up -d --build
```

Esse comando cria e inicia:

| Serviço | Container | Porta |
| --- | --- | --- |
| Aplicação Javalin | `lynotes_app_container` | `8080` |
| PostgreSQL 16 | `spotify_db_container` | `5432` |

O serviço da aplicação aguarda o healthcheck do PostgreSQL antes de iniciar.

Acesse a aplicação em:

```text
http://localhost:8080
```

Credenciais provisórias para testes:

```text
Usuário: admin
Senha: 123456
```

### Comandos úteis

Verificar os containers:

```bash
docker compose ps
```

Acompanhar os logs da aplicação:

```bash
docker compose logs -f app
```

Acompanhar os logs do banco:

```bash
docker compose logs -f db
```

Reconstruir somente a aplicação após alterações:

```bash
docker compose up -d --build app
```

Parar os containers sem apagar os dados:

```bash
docker compose down
```

Parar os containers e apagar o volume do PostgreSQL:

```bash
docker compose down -v
```

O último comando remove permanentemente os dados locais do banco.

### Configuração do banco

O container da aplicação recebe estas variáveis pelo `docker-compose.yaml`:

| Variável | Valor no Docker |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://db:5432/spotify_db` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |

Os dados são mantidos no volume nomeado `postgres_data`. Por isso, reconstruir ou reiniciar os containers não apaga os artistas cadastrados.

## Trabalho Realizado

### Infraestrutura e execução

- Criação de um `Dockerfile` com build em múltiplos estágios.
- Inclusão da aplicação Java no Docker Compose, além do PostgreSQL.
- Configuração de healthcheck para que a aplicação aguarde o banco.
- Configuração das variáveis de conexão por ambiente.
- Persistência dos dados em volume Docker.
- Disponibilização da aplicação na porta `8080` e do banco na porta `5432`.
- Inclusão do `.dockerignore` para reduzir o contexto enviado ao build.
- Criação de um login provisório para permitir os testes das rotas protegidas.

### Funcionalidades de artistas

- Implementação do CRUD completo de artistas.
- Cadastro de artistas com nome, gênero musical e biografia.
- Listagem dos artistas cadastrados.
- Filtro de busca por nome, gênero musical ou biografia.
- Edição utilizando o UUID do artista como identificador.
- Exclusão de artistas.
- Permissão para cadastrar artistas diferentes com o mesmo nome.
- Preservação dos dados preenchidos quando o formulário é inválido.
- Inicialização automática da tabela de artistas.

Rotas implementadas:

| Método | Rota | Responsabilidade |
| --- | --- | --- |
| `GET` | `/artistas` | Listar e filtrar artistas |
| `GET` | `/artistas/novo` | Exibir formulário de cadastro |
| `POST` | `/artistas` | Cadastrar artista |
| `GET` | `/artistas/{id}/editar` | Exibir formulário de edição |
| `POST` | `/artistas/{id}` | Atualizar artista |
| `POST` | `/artistas/{id}/excluir` | Excluir artista |

### Refatoração e orientação a objetos

- Separação das responsabilidades entre controller, service e repository.
- Introdução da interface `ArtistaUseCase` entre a camada HTTP e o serviço.
- Introdução da interface `ArtistaRepository` entre serviço e persistência.
- Injeção de dependências por construtor.
- Criação de um módulo de composição para montar e registrar as dependências.
- Modelo `Artista` imutável, com criação de UUID para novos registros.
- DTO específico para os dados recebidos dos formulários.
- View model específico para a renderização de cadastro e edição.
- Validador dedicado para dados do artista e identificadores.
- Reutilização do processamento e da renderização dos formulários.
- Reutilização das operações JDBC comuns.
- Uso de `Optional` na consulta por identificador.
- Uso de `Objects` para tratamento explícito de referências nulas.
- Remoção de comentários que apenas repetiam o comportamento do código.

### Erros e observabilidade

- Exceções específicas para validação, identificador inválido e artista não encontrado.
- Exceção de infraestrutura para falhas de banco de dados.
- Tratamento centralizado das exceções HTTP.
- Respostas de erro sem exposição de detalhes internos da persistência.
- Logs de cadastro, atualização e exclusão.
- Logs de validação, recurso inexistente e falha de persistência.
- Log de cada requisição com método, caminho e status HTTP.

### Testes implementados

- Testes unitários das regras do `ArtistaService`.
- Teste de integração do repository JDBC.
- Testes das respostas HTTP do controller.
- Testes de renderização dos templates Thymeleaf.
- Cobertura dos status `400`, `404`, `422` e `500`.
- Cobertura do redirecionamento `303` após operações concluídas.
- Validação da aplicação completa com banco e aplicação executando no Docker.

## Estrutura Arquitetural

O código é organizado por domínio, buscando baixo acoplamento e alta coesão. O módulo de artistas segue estas responsabilidades:

- **Model:** representa o artista e protege seu estado.
- **DTO:** transporta e normaliza os dados recebidos do formulário.
- **Controller:** registra rotas, extrai parâmetros e produz respostas HTTP.
- **Use Case:** define o contrato das operações disponíveis para a camada web.
- **Service:** aplica validações e regras de negócio.
- **Repository:** define as operações de persistência.
- **Repository JDBC:** executa exclusivamente consultas e comandos SQL.
- **Validator:** concentra validações de formulário e identificador.
- **Exception Handler:** converte exceções da aplicação em status HTTP.
- **Module:** monta as dependências e registra o módulo no Javalin.

Principais status HTTP utilizados:

| Status | Situação |
| --- | --- |
| `200` | Consulta realizada |
| `303` | Cadastro, atualização ou exclusão concluída |
| `400` | Identificador de artista inválido |
| `404` | Artista não encontrado |
| `422` | Dados do formulário inválidos |
| `500` | Falha interna de persistência |

## Testes Automatizados

Execute a suíte com:

```bash
mvn test
```

Os testes cobrem:

- regras de negócio do serviço de artistas;
- persistência JDBC;
- renderização dos templates;
- rotas e status HTTP;
- serviço e persistência de usuários;
- inicialização da aplicação.

Os testes de banco são ignorados quando não há um PostgreSQL disponível.

## Fluxo de Trabalho

O desenvolvimento colaborativo é orientado por issues e branches de funcionalidade. As alterações devem ser integradas à branch principal por Pull Request com revisão de código.
