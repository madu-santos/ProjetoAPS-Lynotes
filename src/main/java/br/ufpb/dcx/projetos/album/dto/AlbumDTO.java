package br.ufpb.dcx.projetos.album.dto;

import java.util.Objects;

public record AlbumDTO(String titulo, String artista, String anoLancamento, Double nota, String youtubeId, String capaUrl, String resenha) {

    public AlbumDTO(String titulo, String artista, String anoLancamento) {
        this(titulo, artista, anoLancamento, null, null, null, null);
    }

    public AlbumDTO(String titulo, String artista, String anoLancamento, Double nota) {
        this(titulo, artista, anoLancamento, nota, null, null, null);
    }

    public AlbumDTO(String titulo, String artista, String anoLancamento, Double nota, String youtubeId, String capaUrl) {
        this(titulo, artista, anoLancamento, nota, youtubeId, capaUrl, null);
    }

    public AlbumDTO normalizado() {
        return new AlbumDTO(
                normalizarObrigatorio(titulo),
                normalizarObrigatorio(artista),
                normalizarObrigatorio(anoLancamento),
                nota,
                normalizarOpcional(youtubeId),
                normalizarOpcional(capaUrl),
                normalizarOpcional(resenha)
        );
    }

    public void validar() {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("O título é obrigatório");
        }
        if (artista == null || artista.trim().isEmpty()) {
            throw new IllegalArgumentException("O artista é obrigatório");
        }
        try {
            int ano = Integer.parseInt(anoLancamento.trim());
            if (ano < 1900 || ano > 2100) {
                throw new IllegalArgumentException("O ano de lançamento deve estar entre 1900 e 2100");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O ano de lançamento deve ser um número inteiro válido");
        }
    }

    private String normalizarObrigatorio(String valor) {
        return Objects.isNull(valor) ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return Objects.isNull(valor) || valor.isBlank() ? null : valor.trim();
    }
}
