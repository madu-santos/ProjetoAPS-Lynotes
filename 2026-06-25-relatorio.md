# Relatório de Avaliação — EQ09 (APS)

| | |
|---|---|
| **Data** | 2026-06-25 |
| **Repositório** | https://github.com/aps-poo-ufpb/projeto-eq09 |
| **Aplicação** | https://aps-eq09.aps.rodrigor.com |
| **Período de atividade** | 2026-06-22 → 2026-06-22 |
| **Total de commits** (sem merges) | 1 |
| **Integrantes** | Maria Eduarda Santos (@mariaeduarda-png) |

---

## 1. Tecnologias

- Javalin
- Thymeleaf
- MinIO/S3

---

## 2. Análise Funcional

### Endpoints REST (12 mapeados)

| Método | Path | Arquivo |
|--------|------|---------|
| `GET` | `/ping` | `App.java` |
| `GET` | `/login` | `LoginController.java` |
| `POST` | `/login` | `LoginController.java` |
| `POST` | `/logout` | `LoginController.java` |
| `GET` | `/` | `MusicaController.java` |
| `GET` | `/usuarios` | `UsuarioController.java` |
| `GET` | `/usuarios/novo` | `UsuarioController.java` |
| `GET` | `/usuarios/{id}` | `UsuarioController.java` |
| `GET` | `/usuarios/{id}/editar` | `UsuarioController.java` |
| `POST` | `/usuarios` | `UsuarioController.java` |
| `POST` | `/usuarios/{id}` | `UsuarioController.java` |
| `POST` | `/usuarios/{id}/excluir` | `UsuarioController.java` |

### Entidades / Tabelas (2 encontradas)

- `usuario (via schema.sql)`
- `artista (via schema.sql)`

---

## 3. Análise Arquitetural

| Aspecto | Status | Observação |
|---------|--------|-----------|
| Arquitetura em camadas | ✅ | controller=✅  service=✅  repository=✅ |
| Testes automatizados | ✅ | 11 arquivo(s) de teste |
| Migrations versionadas | ❌ | não encontradas |
| Logging | ✅ | @Slf4j / LoggerFactory / logging.getLogger detectado |
| Autenticação / Segurança | ❌ | não detectado |
| DTOs / Separação de dados | ❌ | não detectado |
| Tratamento global de exceções | ❌ | não detectado |
| Documentação de API (OpenAPI) | ❌ | não detectado |
| Variáveis de ambiente | ❌ | não detectado |
| Dockerfile / docker-compose | ✅ | presente |

### Conformidade com requisitos da disciplina (APS)

| Requisito | Status |
|-----------|--------|
| Javalin | ✅ |
| Thymeleaf | ✅ |
| PostgreSQL | ✅ (provisionado pelo servidor) |
| Testes automatizados | ✅ |

---

## 4. Contribuição por Usuário

### Resumo

| Usuário | Commits | % commits | Linhas adicionadas | Linhas no código atual | % código atual |
|---------|---------|-----------|-------------------|----------------------|----------------|
| Maria Eduarda Santos (@mariaeduarda-png) | 1 | 100% | 6.319 | 5.825 | 100% |

### Contribuição por Camada

| Camada | Total linhas | Maria Eduarda Santos (@mariaeduarda-png) |
|--------|-------------|---------|
| Controller | 2.589 | 100% |
| Repository | 1.026 | 100% |
| Service | 1.127 | 100% |
| Test | 123 | 100% |

---

## 5. Contribuição por Funcionalidade

Baseado em `git blame` nos arquivos de controller e service.

| Arquivo | Total linhas | Maria Eduarda Santos (@mariaeduarda-png) |
|---------|-------------|---------|
| `formulario.html` | 434 | 100% |
| `lista.html` | 379 | 100% |
| `MusicaServiceTest.java` | 220 | 100% |
| `MusicaController.java` | 212 | 100% |
| `UsuarioControllerTest.java` | 204 | 100% |
| `index.css` | 197 | 100% |
| `ArtistaControllerTest.java` | 186 | 100% |
| `ArtistaServiceTest.java` | 162 | 100% |
| `LoginControllerTest.java` | 158 | 100% |
| `UsuarioController.java` | 157 | 100% |
| `ArtistaController.java` | 149 | 100% |
| `AlbumServiceTest.java` | 143 | 100% |
| `AlbumController.java` | 134 | 100% |
| `UsuarioService.java` | 111 | 100% |
| `UsuarioServiceTest.java` | 111 | 100% |
| `MusicaService.java` | 96 | 100% |
| `home.html` | 92 | 100% |
| `perfil.html` | 78 | 100% |
| `ArtistaService.java` | 74 | 100% |
| `AlbumService.java` | 65 | 100% |
| `LoginController.java` | 60 | 100% |
| `ArtistaValidator.java` | 52 | 100% |
| `login.html` | 49 | 100% |
| `ArtistaHttpExceptionHandler.java` | 39 | 100% |
| `BCryptHashingStrategy.java` | 33 | 100% |
| `index.html` | 28 | 100% |
| `PlaintextHashingStrategy.java` | 22 | 100% |
| `schema.sql` | 21 | 100% |
| `PasswordHashingStrategy.java` | 19 | 100% |
| `ArtistaUseCase.java` | 19 | 100% |
| `ResourceNotFoundException.java` | 12 | 100% |

---

*Relatório gerado automaticamente em 2026-06-25.*
*Os dados de contribuição são baseados em `git log --numstat` (linhas adicionadas) e `git blame` (linhas no código atual), excluindo commits de merge.*