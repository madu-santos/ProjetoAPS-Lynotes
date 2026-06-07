package br.ufpb.dcx.projetos.login.models;

import java.util.UUID;

/**
 * Entidade de Domínio representando um Usuário do sistema.
 * Segue o padrão JavaBean (POJO - Plain Old Java Object) com construtores, getters e setters.
 */
public class Usuario {

    private String id;
    private String nome;
    private String email;
    private String senha; // Guardará o hash seguro da senha gerado pelo BCrypt

    // Construtor completo usado para instanciar objetos vindo do Banco de Dados
    public Usuario(String id, String nome, String email, String senha) {
        this.id = id;
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    // Construtor auxiliar sem o ID, gerando o UUID automaticamente (usado no cadastro de novos usuários)
    public Usuario(String nome, String email, String senha) {
        this.id = UUID.randomUUID().toString(); // Gera um identificador único universal
        this.nome = nome;
        this.email = email;
        this.senha = senha;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }
}
