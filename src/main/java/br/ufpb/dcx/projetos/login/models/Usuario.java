package br.ufpb.dcx.projetos.login.models;

import java.util.UUID;

/**
 * Entidade de Domínio representando um Usuário do sistema.
 * Segue o padrão JavaBean (POJO - Plain Old Java Object) com construtores, getters e setters.
 */
public class Usuario {

    private String id;
    private String nome;
    private String username;
    private String email;
    private String senha; // Guardará o hash seguro da senha gerado pelo BCrypt
    private String tipo; // ADMIN ou COMUM
    private String bio;
    private String fotoUrl;

    // Construtor completo usado para instanciar objetos vindo do Banco de Dados
    public Usuario(String id, String nome, String username, String email, String senha, String tipo, String bio, String fotoUrl) {
        this.id = id;
        this.nome = nome;
        this.username = username;
        this.email = email;
        this.senha = senha;
        this.tipo = tipo;
        this.bio = bio;
        this.fotoUrl = fotoUrl;
    }

    // Sobrecargas para compatibilidade
    public Usuario(String id, String nome, String username, String email, String senha, String tipo, String bio) {
        this(id, nome, username, email, senha, tipo, bio, null);
    }

    public Usuario(String id, String nome, String email, String senha, String tipo) {
        this(id, nome, null, email, senha, tipo, null, null);
    }

    public Usuario(String id, String nome, String email, String senha, String tipo, String bio) {
        this(id, nome, null, email, senha, tipo, bio, null);
    }

    // Construtor auxiliar sem o ID, gerando o UUID automaticamente (usado no cadastro de novos usuários)
    public Usuario(String nome, String username, String email, String senha) {
        this.id = UUID.randomUUID().toString(); // Gera um identificador único universal
        this.nome = nome;
        this.username = username;
        this.email = email;
        this.senha = senha;
        this.tipo = "COMUM";
        this.bio = "";
        this.fotoUrl = "";
    }

    // Sobrecarga auxiliar para compatibilidade legada (sem username)
    public Usuario(String nome, String email, String senha) {
        this(nome, null, email, senha);
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getFotoUrl() {
        return fotoUrl;
    }

    public void setFotoUrl(String fotoUrl) {
        this.fotoUrl = fotoUrl;
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

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }
}
