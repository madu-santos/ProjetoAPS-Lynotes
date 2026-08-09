# 🎵 Lynotes - Painel Administrativo & Comunidade Musical

O **Lynotes** é o projeto final da disciplina de POO (Equipe 09). Ele é muito mais que um painel administrativo tradicional: é um "Spotify Backoffice" que mistura a catalogação rigorosa de **Músicas, Álbuns, Artistas e Playlists** com uma dinâmica imersiva de **Rede Social**.

Os usuários podem explorar o catálogo com integração direta ao YouTube (Player embutido), avaliar músicas, acompanhar rankings de popularidade e compartilhar suas resenhas de obras com seus seguidores através do Feed da Comunidade (estilo Twitter).

---

### 📺 Vídeo de Apresentação
Assista à nossa demonstração prática e tour pela arquitetura no YouTube:
**[Acesse o Vídeo de Apresentação no YouTube](https://youtu.be/hnuIOO55QWY)**

---

## 🛠️ Tecnologias Principais
- **Back-end:** Java 21, Javalin 6 (MVC), JDBC, Padrões de Projeto (Factory, Strategy, Decorator).
- **Front-end:** HTML, CSS (Dark Mode Premium), HTMX, JavaScript (Integração YouTube API).
- **Infra e Dados:** PostgreSQL 16, Flyway (Migrations), Docker & Docker Compose.
- **Ferramentas:** Swagger (OpenAPI) e Logback.

---

## 🚀 Como Executar o Projeto (Docker)

**1.** Na raiz do projeto, suba os contêineres:
```bash
docker compose up -d --build
```

**2.** Acesse o painel pelo navegador:
```text
http://localhost:8080
```
*(A documentação interativa da API estará disponível em `/swagger`)*

**3.** Credenciais Administrativas de Acesso Inicial (Auto-Seed):
```text
E-mail: admin@email.com
Senha: admin123
```

---

## 🏗️ Arquitetura e Estrutura
O sistema foi desenhado visando escalabilidade e clean code:
- **Separação em Módulos:** Dividido por contextos (`comunidade`, `login`, `musica`, `album`, `playlist`, `rankings`).
- **Padrão MVC e SOLID:** Separação clara entre Controllers HTTP, Services (Regra de Negócio), Repositories (Banco de Dados) e Models/DTOs.
- **Design Patterns Aplicados:** Implementação real dos padrões estruturais e comportamentais do GoF.
- **Tratamento de Exceções:** Sistema global que intercepta erros e redireciona os clientes sem exibir StackTraces expostos.
