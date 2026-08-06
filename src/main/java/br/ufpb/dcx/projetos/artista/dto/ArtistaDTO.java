package br.ufpb.dcx.projetos.artista.dto;

import java.time.LocalDate;
import java.util.Objects;

public record ArtistaDTO(String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, Boolean solicitarVerificacao, Double nota, String youtubeId, String capaUrl) {

    public ArtistaDTO(String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, Boolean solicitarVerificacao) {
        this(nome, generoMusical, biografia, cpf, dataNascimento, cep, logradouro, numero, bairro, cidade, uf, solicitarVerificacao, null, null, null);
    }

    public ArtistaDTO(String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, Boolean solicitarVerificacao, Double nota) {
        this(nome, generoMusical, biografia, cpf, dataNascimento, cep, logradouro, numero, bairro, cidade, uf, solicitarVerificacao, nota, null, null);
    }

    public ArtistaDTO normalizado() {
        return new ArtistaDTO(
                normalizarObrigatorio(nome),
                normalizarOpcional(generoMusical),
                normalizarOpcional(biografia),
                normalizarOpcional(cpf),
                dataNascimento,
                normalizarOpcional(cep),
                normalizarOpcional(logradouro),
                normalizarOpcional(numero),
                normalizarOpcional(bairro),
                normalizarOpcional(cidade),
                normalizarOpcional(uf),
                solicitarVerificacao != null ? solicitarVerificacao : false,
                nota,
                normalizarOpcional(youtubeId),
                normalizarOpcional(capaUrl)
        );
    }

    private String normalizarObrigatorio(String valor) {
        return Objects.isNull(valor) ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return Objects.isNull(valor) || valor.isBlank() ? null : valor.trim();
    }
}
