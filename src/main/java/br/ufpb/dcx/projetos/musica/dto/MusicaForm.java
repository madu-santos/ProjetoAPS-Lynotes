package br.ufpb.dcx.projetos.musica.dto;

import java.util.Objects;

public record MusicaForm(
        String titulo,
        String artista,
        String duracaoSegundos,
        String albumId,
        String nota,
        String resenha,
        String spotifyUrl
) {

    public MusicaForm normalizado() {
        return new MusicaForm(
                normalizarObrigatorio(titulo),
                normalizarObrigatorio(artista),
                normalizarOpcional(duracaoSegundos),
                normalizarOpcional(albumId),
                normalizarOpcional(nota),
                normalizarOpcional(resenha),
                normalizarOpcional(spotifyUrl)
        );
    }

    private String normalizarObrigatorio(String valor) {
        return Objects.isNull(valor) ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return Objects.isNull(valor) || valor.isBlank() ? null : valor.trim();
    }
}
