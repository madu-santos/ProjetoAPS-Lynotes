# 🚀 Guia de Desenvolvimento — Spotify Backoffice (APS/POO)

Bem-vindo ao repositório base do **Spotify Backoffice**! Este projeto serve como o esqueleto inicial ("Walking Skeleton") para o desenvolvimento do nosso projeto da disciplina. A base já está configurada com **Docker (PostgreSQL)**, **Javalin 6**, **Thymeleaf 3** e **JUnit 5** para testes automatizados.

Abaixo está o guia detalhado de configuração do ambiente e de como desenvolver novas funcionalidades seguindo a arquitetura estabelecida.

---

## 📌 1. Pré-requisitos

Antes de iniciar, certifique-se de possuir instalado em sua máquina:
1. **Java JDK 21** (ou superior).
2. **Docker Desktop** (com integração WSL2 ativa se você utiliza Windows).
3. **IntelliJ IDEA** (ou IDE de sua preferência configurada com suporte a Maven).

---

## 📥 2. Como Rodar o Projeto pela Primeira Vez

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/mariaeduarda-png/my-own-spotify.git
   ```
2. **Abra na IDE:** Abra a pasta do projeto no IntelliJ como um projeto Maven. A IDE baixará automaticamente todas as dependências declaradas no `pom.xml`.
3. **Inicie o Banco de Dados (Docker):**
   * Certifique-se de que o **Docker Desktop** está aberto e rodando.
   * No terminal do IntelliJ, execute o comando na raiz do projeto:
     ```bash
     docker compose up -d
     ```
   * Isso inicializará um contêiner PostgreSQL 16 escutando localmente na porta `5432` com as credenciais `postgres/postgres`.
4. **Execute os Testes:** Para validar se tudo está funcionando (inclusive a conexão com o banco de dados), execute:
   ```bash
   mvn test
   ```
   *Você deverá receber uma mensagem de `BUILD SUCCESS` confirmando que todos os testes foram executados e passaram.*

---

## 📐 3. Arquitetura do Projeto

Para garantir desacoplamento, baixo acoplamento e facilidade na hora de testar, adotamos a organização **por conceito/domínio** (Package-by-Concept). O módulo de `login` serve como o modelo oficial do projeto:

* **`models/`**: Classes de entidade (Ex: `Usuario.java`).
* **`repositories/`**: 
  * `UsuarioRepository.java` (Interface): Abstração que declara as operações de dados.
  * `UsuarioDbRepository.java` (JDBC): Implementação física que executa SQL puro no PostgreSQL usando JDBC.
* **`services/`**: Camada que centraliza as regras de negócio e validações (Ex: `UsuarioService.java`, que impede senhas curtas, e-mails duplicados ou sem formatação correta).

---

## 🛠️ 4. Passo a Passo para Desenvolver o Seu Domínio (Ex: Artista ou Música)

Cada integrante do grupo desenvolverá um módulo vertical independente. Siga os passos abaixo (usando o exemplo do domínio **Artista**):

### Passo 1: Atualizar o Banco de Dados
Adicione a query SQL de criação da sua tabela no arquivo `src/main/resources/schema.sql`.
> ⚠️ **Atenção:** Ao editar o arquivo `schema.sql`, você deve desligar e reiniciar o banco no Docker para reconstruir as tabelas:
> ```bash
> docker compose down
> docker compose up -d
> ```

### Passo 2: Criar os Pacotes
Crie o pacote do seu domínio dentro da estrutura Java (Ex: `br.ufpb.dcx.projetos.artista`) com as seguintes subpastas:
* `models`
* `repositories`
* `services`

### Passo 3: Implementar o Código
Siga estritamente o modelo do pacote de `login`:
1. Desenvolva seu **Model** (Ex: `Artista.java`).
2. Defina a **Interface** (Ex: `ArtistaRepository.java`).
3. Crie a implementação **JDBC** (Ex: `ArtistaDbRepository.java`). Utilize o padrão de conexão e try/catch com SQL puro presente no repositório de usuários.
4. Crie a classe de **Serviço** (Ex: `ArtistaService.java`) e implemente as validações necessárias para impedir dados inválidos de irem ao banco.

### Passo 4: Escrever Testes Automatizados
Crie uma pasta correspondente na estrutura de testes (`src/test/java/br/ufpb/dcx/projetos/artista`):
* **Teste de Banco (Integração):** Crie a classe (Ex: `ArtistaDbRepositoryTest.java`). Copie o cabeçalho com a verificação rápida de conexão (`assumeTrue`) para evitar que seu teste quebre o build do Maven caso o Docker de alguém esteja offline.
* **Teste de Negócio (Unitário):** Crie a classe (Ex: `ArtistaServiceTest.java`) e implemente uma simulação em memória do repositório (Fake Repository) para testar suas validações de serviço sem depender do Docker (deixando o teste ultrarrápido).

---

## ⚙️ 5. Boas Práticas do Git e Ramificações

* **Não trabalhe diretamente na branch `main`!**
* Sempre crie uma branch de tarefas a partir da `main` atualizada antes de iniciar o desenvolvimento:
  ```bash
  git checkout -b feature-artistas
  # ou
  git checkout -b feature-musicas
  ```
* Ao concluir e verificar que todos os testes continuam passando (`mvn test`), envie sua branch para o GitHub e abra um **Pull Request (PR)** para revisão do grupo antes de integrar à branch principal.
