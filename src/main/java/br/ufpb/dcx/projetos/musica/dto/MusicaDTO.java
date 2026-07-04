package br.ufpb.dcx.projetos.musica.dto;

import java.util.Objects;

public record MusicaDTO(
        String titulo,
        String artista,
        String duracaoSegundos,
        String albumId,
        String nota,
        String resenha,
        String spotifyUrl,
        String youtubeUrl,
        String playlistId,
        String novaPlaylistNome
) {

    public MusicaDTO normalizado() {
        return new MusicaDTO(
                normalizarObrigatorio(titulo),
                normalizarObrigatorio(artista),
                normalizarOpcional(duracaoSegundos),
                normalizarOpcional(albumId),
                normalizarOpcional(nota),
                normalizarOpcional(resenha),
                normalizarOpcional(spotifyUrl),
                normalizarOpcional(youtubeUrl),
                normalizarOpcional(playlistId),
                normalizarOpcional(novaPlaylistNome)
        );
    }

    public void validar() {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("O título da música é obrigatório");
        }
        if (artista == null || artista.trim().isEmpty()) {
            throw new IllegalArgumentException("O artista é obrigatório");
        }
        if (nota != null && !nota.isBlank()) {
            try {
                int notaVal = Integer.parseInt(nota);
                if (notaVal < 1 || notaVal > 5) {
                    throw new IllegalArgumentException("A nota deve ser entre 1 e 5 estrelas");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("A nota deve ser um número inteiro válido");
            }
        }
    }

    private String normalizarObrigatorio(String valor) {
        return Objects.isNull(valor) ? null : valor.trim();
    }

    private String normalizarOpcional(String valor) {
        return Objects.isNull(valor) || valor.isBlank() ? null : valor.trim();
    }
}
