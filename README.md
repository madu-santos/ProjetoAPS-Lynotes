# 🎵 Lynotes - Painel Administrativo & Comunidade Musical

O **Lynotes** é o projeto final da disciplina de POO (Equipe 09). Ele é muito mais que um painel administrativo tradicional: é um "Spotify Backoffice" que mistura a catalogação rigorosa de **Músicas, Álbuns, Artistas e Playlists** com uma dinâmica imersiva de **Rede Social**.

Os usuários podem explorar o catálogo com integração direta a APIs públicas (Deezer/iTunes) e player embutido do YouTube, avaliar músicas, acompanhar rankings de popularidade, interagir via notificações e compartilhar suas resenhas de obras com seus seguidores através da Comunidade.

---

### 📺 Vídeo de Apresentação
Assista à nossa demonstração prática e tour pela arquitetura no YouTube:
**[Acesse o Vídeo de Apresentação no YouTube](https://youtu.be/hnuIOO55QWY)**

---

## 🛠️ Tecnologias Principais
- **Back-end:** Java 21, Javalin 6 (MVC), JDBC, Padrões de Projeto (Factory, Strategy, Decorator, Observer).
- **Front-end:** HTML5, CSS3 (Dark Mode Cyber), HTMX, JavaScript (Integração YouTube API & Deezer API).
- **Infra e Dados:** PostgreSQL 16, Flyway (Migrations), Docker & Docker Compose.
- **Ferramentas:** Swagger (OpenAPI), SLF4J / Logback.

---

## 🌟 Funcionalidades em Destaque

- **🔔 Sistema de Notificações:** Notificações em tempo real sobre novos seguidores, curtidas/estrelas em postagens e comentários, com ações diretas como *"Seguir de volta"*.
- **💿 Mini Player Interativo:** Player com animação de disco de vinil girando e capa central (label de 40px), painel retrátil, movimentação livre pela tela (modo flutuante) e sincronização entre telas.
- **🔍 Catalogação Inteligente via APIs:** Busca ao vivo conectada às APIs do Deezer e iTunes que preenche automaticamente metadados (capas em alta resolução, artistas, duração e IDs).
- **💬 Comunidade & Feed:** Compartilhamento de resenhas públicas, sistema de seguidores, curtidas e comentários estilo rede social.
- **🏆 Rankings da Comunidade:** Classificação oficial das músicas, álbuns e artistas mais bem avaliados e ouvidos da plataforma.

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
- **Design Patterns Aplicados:** Implementação real dos padrões estruturais e comportamentais do GoF (Factory Method, Strategy, Decorator, Observer).
- **Tratamento de Exceções:** Sistema global que intercepta erros e redireciona os clientes sem exibir StackTraces expostos.
