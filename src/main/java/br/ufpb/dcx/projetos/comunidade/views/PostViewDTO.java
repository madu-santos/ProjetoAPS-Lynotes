package br.ufpb.dcx.projetos.comunidade.views;

import java.time.LocalDateTime;

public record PostViewDTO(
    String id,
    String usuarioId,
    String usuarioNome,
    String usuarioUsername,
    String usuarioFotoUrl,
    String conteudo,
    LocalDateTime dataCriacao,
    int qtdEstrelas,
    int qtdComentarios,
    boolean estreladoPorMim,
    String tipo,
    String musicaId,
    String musicaTitulo,
    String musicaArtista,
    Double musicaNota,
    String musicaYoutubeUrl,
    String albumId,
    String albumTitulo,
    String albumArtista,
    Double albumNota,
    String artistaId,
    String artistaNome
) {
    public PostViewDTO(String id, String usuarioId, String usuarioNome, String conteudo, LocalDateTime dataCriacao, int qtdEstrelas, int qtdComentarios, boolean estreladoPorMim) {
        this(id, usuarioId, usuarioNome, null, null, conteudo, dataCriacao, qtdEstrelas, qtdComentarios, estreladoPorMim, "TEXTO", null, null, null, null, null, null, null, null, null, null, null);
    }
}
