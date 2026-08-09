package br.ufpb.dcx.projetos.playlist.views;

public record PlaylistItemView(
    String id,
    String videoId,
    String url,
    String titulo,
    String artista,
    String capaUrl,
    boolean oculta
) {}
