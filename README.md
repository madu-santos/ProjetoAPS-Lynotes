# Lynotes — Painel Administrativo (APS/POO)

Este repositório contém a entrega inicial do **Lynotes**, um painel administrativo web para gerenciamento e catalogação de músicas, artistas e álbuns. O foco desta etapa ("Walking Skeleton") foi estabelecer a arquitetura do projeto e mitigar os riscos de infraestrutura técnica.

---

## 1. Tecnologias Utilizadas
* **Linguagem:** Java 21
* **Framework Web:** Javalin 6 (com servidor Jetty embutido)
* **Motor de Renderização (Front-end):** Thymeleaf 3 + Bootstrap 5
* **Banco de Dados Relacional:** PostgreSQL 16
* **Ambiente de Execução Local:** Docker & Docker Compose

---

## 2. Estrutura Arquitetural
Adotamos uma abordagem baseada em baixo acoplamento e alta coesão, organizando o código **por conceito/domínio** (Package-by-Concept). O módulo de `login`/`usuário` serve como modelo de referência arquitetural:

* **Model (`models/`):** Entidades puras do domínio com encapsulamento de estado (Ex: `Usuario`).
* **Repository (`repositories/`):** 
  * `UsuarioRepository` (Interface): Abstração que atua como barreira de isolamento (Inversão de Dependência - DIP do SOLID).
  * `UsuarioDbRepository` (Classe): Implementação física que executa queries SQL puras no PostgreSQL via **JDBC puro** (sem o uso de ORMs complexos para garantir controle total sobre as conexões e consultas).
* **Service (`services/`):** Concentra as regras de negócio e validações rígidas de campos (como integridade de e-mails, unicidade e tamanho mínimo de senhas).

---

## 3. Qualidade e Testes Automatizados
O projeto conta com testes automatizados desenvolvidos com **JUnit 5**:

1. **Testes Unitários de Serviço (`UsuarioServiceTest`):** Validam as regras de negócio de maneira isolada e ultrarrápida usando um repositório simulado em memória (*Fake Repository*), sem dependência do banco real.
2. **Testes de Integração de Banco (`UsuarioDbRepositoryTest`):** Testam a gravação e integridade de dados no PostgreSQL do Docker. Utilizam a cláusula `assumeTrue` para verificar se o banco local está ativo. Caso o Docker esteja desligado, o JUnit ignora o teste silenciosamente, impedindo que o build do projeto quebre.
3. **Testes de Integração de Rotas (`AppTest`):** Testam as requisições HTTP e o retorno dos endpoints expostos.

---

## 4. Fluxo de Trabalho (Git)
O desenvolvimento colaborativo é orientado por **Issues** e organizado em branches de funcionalidades (ex: `feature-artistas`), integrando alterações na branch principal (`main`) exclusivamente através de **Pull Requests** com revisão de código.
