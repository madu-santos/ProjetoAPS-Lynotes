package br.ufpb.dcx.projetos.artista.dto;

import java.time.LocalDate;
import java.util.Objects;

public record ArtistaDTO(String nome, String generoMusical, String biografia, String cpf, LocalDate dataNascimento, String cep, String logradouro, String numero, String bairro, String cidade, String uf, Boolean solicitarVerificacao) {

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
                solicitarVerificacao != null ? solicitarVerificacao : false
        );
    }

    private String normalizarObrigatorio(String valor) {
        return Objects.isNull(valor) ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return Objects.isNull(valor) || valor.isBlank() ? null : valor.trim();
    }
}
