# 🚀 Lynotes — Painel de Controle (APS/POO)

Este repositório contém a base funcional ("Walking Skeleton") do **Lynotes**, nosso painel administrativo de músicas, artistas e álbuns. A aplicação está configurada com Java 21, **Javalin 6**, **Thymeleaf 3**, **Docker (PostgreSQL)** e **JUnit 5** para testes automatizados.

---

## 📌 1. Pré-requisitos
* Java JDK 21 (ou superior)
* Docker Desktop
* IntelliJ IDEA (ou qualquer IDE Java com suporte a Maven)

---

## 📥 2. Como Rodar o Projeto
1. Clone o repositório oficial.
2. Abra a pasta do projeto no IntelliJ como um projeto Maven.
3. Inicie o banco de dados no Docker executando na raiz do projeto:
   ```bash
   docker compose up -d
   ```
4. Execute os testes automatizados para validar a compilação e a conexão de banco:
   ```bash
   mvn test
   ```

---

## 📐 3. Estrutura e Arquitetura do Projeto
Adotamos uma organização **por conceito/domínio** (Package-by-Concept) para garantir modularidade e baixo acoplamento. O pacote `login` serve como o modelo do projeto:
* `models/`: Entidades conceituais.
* `repositories/`: Interface de persistência + Implementação JDBC (SQL puro).
* `services/`: Lógica e validações rigorosas de negócio.

Os testes estão divididos em **Testes de Integração** (banco real no Docker) e **Testes Unitários** (lógica de negócios simulada em memória para velocidade).

---

## ⚙️ 4. Fluxo de Trabalho do Git (Obrigatório)
1. **Não trabalhe direto na `main`!**
2. Crie uma branch para a sua funcionalidade (ex: `feature-artistas`, `feature-musicas`).
3. Ao terminar de codificar e garantir que todos os testes passam (`mvn test`), envie a branch para o repositório e abra um **Pull Request (PR)** para revisão do grupo antes de realizar o merge.
