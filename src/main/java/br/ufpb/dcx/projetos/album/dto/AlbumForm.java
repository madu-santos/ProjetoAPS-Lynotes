package br.ufpb.dcx.projetos.album.dto;

import java.util.Objects;

public record AlbumForm(String titulo, String artista, String anoLancamento) {

    public AlbumForm normalizado() {
        return new AlbumForm(
                normalizarObrigatorio(titulo),
                normalizarObrigatorio(artista),
                normalizarObrigatorio(anoLancamento)
        );
    }

    private String normalizarObrigatorio(String valor) {
        return Objects.isNull(valor) ? null : valor.trim();
    }
}
