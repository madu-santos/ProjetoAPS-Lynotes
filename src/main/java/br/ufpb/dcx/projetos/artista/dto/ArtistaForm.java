package br.ufpb.dcx.projetos.artista.dto;

import java.util.Objects;

public record ArtistaForm(String nome, String generoMusical, String biografia) {

    public ArtistaForm normalizado() {
        return new ArtistaForm(
                normalizarObrigatorio(nome),
                normalizarOpcional(generoMusical),
                normalizarOpcional(biografia)
        );
    }

    private String normalizarObrigatorio(String valor) {
        return Objects.isNull(valor) ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return Objects.isNull(valor) || valor.isBlank() ? null : valor.trim();
    }
}
