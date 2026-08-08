package br.ufpb.dcx.projetos.comunidade.services;

import br.ufpb.dcx.projetos.comunidade.models.Post;

import java.time.LocalDateTime;
import java.util.UUID;

public final class PostFactory {

    private PostFactory() {
    }

    public static Post criarPostTexto(String usuarioId, String conteudo) {
        return criarPost(usuarioId, conteudo, "TEXTO", null, null, null);
    }

    public static Post criarPostMusica(String usuarioId, String musicaId, String conteudo) {
        return criarPostMusica(usuarioId, musicaId, conteudo, "SHARE_MUSIC");
    }

    public static Post criarPostMusica(String usuarioId, String musicaId, String conteudo, String tipo) {
        return criarPost(usuarioId, conteudo, tipo, musicaId, null, null);
    }

    public static Post criarPostAlbum(String usuarioId, String albumId, String conteudo) {
        return criarPostAlbum(usuarioId, albumId, conteudo, "SHARE_ALBUM");
    }

    public static Post criarPostAlbum(String usuarioId, String albumId, String conteudo, String tipo) {
        return criarPost(usuarioId, conteudo, tipo, null, albumId, null);
    }

    private static Post criarPost(String usuarioId, String conteudo, String tipo, String musicaId, String albumId, String artistaId) {
        return new Post(
                UUID.randomUUID().toString(),
                usuarioId,
                normalizarConteudo(conteudo),
                LocalDateTime.now(),
                tipo,
                musicaId,
                albumId,
                artistaId
        );
    }

    private static String normalizarConteudo(String conteudo) {
        return conteudo == null ? "" : conteudo.trim();
    }
}

