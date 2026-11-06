# 🎵 Lynotes

> Plataforma web de catalogação e acervo musical unificado com experiência social integrada, inspirada no ecossistema do Spotify.

[![Demonstração em Vídeo](https://img.youtube.com/vi/QjDpExiqm0Q/maxresdefault.jpg)](https://youtu.be/QjDpExiqm0Q)

*Clique na imagem acima para assistir à demonstração da aplicação em funcionamento no YouTube.*

---

## 📌 Sobre o Projeto

O **Lynotes** resolve o desafio de centralizar a gestão de um catálogo musical pessoal com a interatividade de uma comunidade ativa. Além de permitir a catalogação detalhada de músicas, álbuns, artistas e playlists, a aplicação transforma avaliações e inclusões em publicações dinâmicas em um feed social, permitindo interação entre ouvintes.

---

## 🌟 Funcionalidades

- **Catálogo Unificado:** Gerenciamento completo (CRUD) de Músicas, Álbuns, Artistas e Playlists com capas dinâmicas.
- **Player Multimídia Contínuo:** Mini-player flutuante integrado à API do YouTube, persistente durante a navegação entre páginas via requisições assíncronas.
- **Busca e Metadados:** Integração com APIs externas para preenchimento de capas em alta resolução e informações de faixas.
- **Feed da Comunidade:** Publicações geradas a partir de avaliações, sistema de estrelas (curtidas), comentários e relação de seguidores.
- **Rankings Dinâmicos:** Classificação em tempo real das músicas, álbuns e artistas mais bem avaliados e executados.
- **Segurança e Controle de Acesso:** Autenticação de sessões, criptografia de senhas e perfis de permissão (Administrador e Usuário Comum).

---

## 🛠️ Tecnologias Utilizadas

| Camada | Tecnologias |
| :--- | :--- |
| **Back-end** | Java 21, Javalin 6 (Arquitetura MVC), JDBC nativo |
| **Front-end** | Thymeleaf 3, HTMX, HTML5, CSS3 (Tema Dark Cyber), Bootstrap 5, JavaScript Vanilla |
| **Banco de Dados** | PostgreSQL 16, Flyway (Database Migrations) |
| **Infraestrutura** | Docker, Docker Compose, Maven |
| **Documentação & Logs** | OpenAPI / Swagger UI, SLF4J com Logback |
| **Testes** | JUnit 5, Mockito |

---

## 🏗️ Arquitetura e Padrões de Projeto

O projeto foi estruturado seguindo princípios de **Clean Code** e separação de responsabilidades (Controller, Service, Repository, DTO/View):

- **Factory Method (`PostFactory`):** Centraliza a criação de publicações no feed de acordo com o tipo de item catalogado.
- **Strategy (`RankingStrategy` e `PasswordHashingStrategy`):** Algoritmos intercambiáveis para cálculos de rankings e métodos de hash (BCrypt).
- **Decorator (`LoggingRepositoryDecorator`):** Auditoria e interceptação transparente de chamadas nas operações de persistência sem acoplamento.
- **Observer (`ComunidadeObserver`):** Disparo de notificações e sincronização de eventos sociais.

---

## 🚀 Como Executar Localmente

### Pré-requisitos
- [Docker](https://www.docker.com/) e Docker Compose instalados.

### Passo a Passo

1. **Clone o repositório:**
   ```bash
   git clone https://github.com/madu-santos/ProjetoAPS-Lynotes.git
   cd ProjetoAPS-Lynotes
   ```

2. **Suba os serviços via Docker:**
   ```bash
   docker compose up -d --build
   ```

3. **Acesse no navegador:**
   - Aplicação: [http://localhost:8080](http://localhost:8080)
   - Documentação da API: [http://localhost:8080/swagger](http://localhost:8080/swagger)

*(Para credenciais de acesso local em ambiente de desenvolvimento, consulte as migrações em `src/main/resources/db/migration` ou utilize o formulário de cadastro).*

---

## 📺 Apresentação e Demonstração

- **Vídeo de Apresentação:** [Assista no YouTube](https://youtu.be/QjDpExiqm0Q)

---

## 🎓 Contexto Acadêmico

- **Instituição:** Universidade Federal da Paraíba (UFPB) - Centro de Informática (DCX)
- **Equipe (Grupo 09):** Maria Eduarda, Maiza Moura e Jefferson Filho
