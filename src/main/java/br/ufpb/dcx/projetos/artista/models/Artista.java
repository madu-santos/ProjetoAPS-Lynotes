package br.ufpb.dcx.projetos.artista.models;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public final class Artista {

    private final String id;
    private final String nome;
    private final String generoMusical;
    private final String biografia;
    private final String cpf;
    private final LocalDate dataNascimento;
    private final String cep;
    private final String logradouro;
    private final String numero;
    private final String bairro;
    private final String cidade;
    private final String uf;
    private final StatusVerificacao statusVerificacao;
    private final Double nota;
    private final String usuarioId;
    private final String youtubeId;
    private final String capaUrl;

    public Artista(String id, String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, StatusVerificacao statusVerificacao, Double nota, String usuarioId, String youtubeId, String capaUrl) {
        this.id = Objects.requireNonNull(id);
        this.nome = nome;
        this.generoMusical = generoMusical;
        this.biografia = biografia;
        this.cpf = cpf;
        this.dataNascimento = dataNascimento;
        this.cep = cep;
        this.logradouro = logradouro;
        this.numero = numero;
        this.bairro = bairro;
        this.cidade = cidade;
        this.uf = uf;
        this.statusVerificacao = statusVerificacao;
        this.nota = nota;
        this.usuarioId = usuarioId;
        this.youtubeId = youtubeId;
        this.capaUrl = capaUrl;
    }

    public Artista(String id, String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, StatusVerificacao statusVerificacao, Double nota, String usuarioId) {
        this(id, nome, generoMusical, biografia, cpf, dataNascimento, cep, logradouro, numero, bairro, cidade, uf, statusVerificacao, nota, usuarioId, null, null);
    }

    public Artista(String id, String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, StatusVerificacao statusVerificacao, String usuarioId) {
        this(id, nome, generoMusical, biografia, cpf, dataNascimento, cep, logradouro, numero, bairro, cidade, uf, statusVerificacao, null, usuarioId, null, null);
    }

    public static Artista novo(String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, StatusVerificacao statusVerificacao, String usuarioId) {
        return new Artista(UUID.randomUUID().toString(), nome, generoMusical, biografia, cpf, dataNascimento, cep, logradouro, numero, bairro, cidade, uf, statusVerificacao, null, usuarioId, null, null);
    }

    public static Artista novo(String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, StatusVerificacao statusVerificacao, Double nota, String usuarioId) {
        return new Artista(UUID.randomUUID().toString(), nome, generoMusical, biografia, cpf, dataNascimento, cep, logradouro, numero, bairro, cidade, uf, statusVerificacao, nota, usuarioId, null, null);
    }

    public static Artista novo(String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, StatusVerificacao statusVerificacao, Double nota, String usuarioId, String youtubeId, String capaUrl) {
        return new Artista(UUID.randomUUID().toString(), nome, generoMusical, biografia, cpf, dataNascimento, cep, logradouro, numero, bairro, cidade, uf, statusVerificacao, nota, usuarioId, youtubeId, capaUrl);
    }

    public Artista comDados(String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, StatusVerificacao statusVerificacao) {
        return new Artista(id, nome, generoMusical, biografia, cpf, dataNascimento, cep, logradouro, numero, bairro, cidade, uf, statusVerificacao, nota, usuarioId, youtubeId, capaUrl);
    }

    public Artista comDados(String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, StatusVerificacao statusVerificacao, Double nota) {
        return new Artista(id, nome, generoMusical, biografia, cpf, dataNascimento, cep, logradouro, numero, bairro, cidade, uf, statusVerificacao, nota, usuarioId, youtubeId, capaUrl);
    }

    public String getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getGeneroMusical() {
        return generoMusical;
    }

    public String getBiografia() {
        return biografia;
    }

    public String getCpf() {
        return cpf;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public String getCep() {
        return cep;
    }

    public String getLogradouro() {
        return logradouro;
    }

    public String getNumero() {
        return numero;
    }

    public String getBairro() {
        return bairro;
    }

    public String getCidade() {
        return cidade;
    }

    public String getUf() {
        return uf;
    }

    public StatusVerificacao getStatusVerificacao() {
        return statusVerificacao;
    }

    public Double getNota() {
        return nota;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public String getCapaUrl() {
        return capaUrl;
    }
}

