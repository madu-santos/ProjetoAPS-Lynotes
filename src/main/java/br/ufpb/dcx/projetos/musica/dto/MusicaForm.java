package br.ufpb.dcx.projetos.musica.dto;

import java.util.Objects;

public record MusicaForm(String titulo, String genero, String duracaoSegundos, String albumId) {

    public MusicaForm normalizado() {
        return new MusicaForm(
                normalizarObrigatorio(titulo),
                normalizarObrigatorio(genero),
                normalizarObrigatorio(duracaoSegundos),
                normalizarOpcional(albumId)
        );
    }

    private String normalizarObrigatorio(String valor) {
        return Objects.isNull(valor) ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return Objects.isNull(valor) || valor.isBlank() ? null : valor.trim();
    }
}
