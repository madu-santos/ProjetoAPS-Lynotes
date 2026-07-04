package br.ufpb.dcx.projetos.album.dto;

import java.util.Objects;

public record AlbumDTO(String titulo, String artista, String anoLancamento) {

    public AlbumDTO normalizado() {
        return new AlbumDTO(
                normalizarObrigatorio(titulo),
                normalizarObrigatorio(artista),
                normalizarObrigatorio(anoLancamento)
        );
    }

    private String normalizarObrigatorio(String valor) {
        return Objects.isNull(valor) ? null : valor.trim();
    }
}
