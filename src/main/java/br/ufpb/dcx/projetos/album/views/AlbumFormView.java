package br.ufpb.dcx.projetos.album.views;

import br.ufpb.dcx.projetos.album.dto.AlbumDTO;
import br.ufpb.dcx.projetos.album.models.Album;

import java.util.Objects;

public final class AlbumFormView {

    private final String id;
    private final String titulo;
    private final String artista;
    private final String anoLancamento;
    private final boolean edicao;
    private final String acao;

    private AlbumFormView(
            String id,
            String titulo,
            String artista,
            String anoLancamento,
            boolean edicao,
            String acao
    ) {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.anoLancamento = anoLancamento;
        this.edicao = edicao;
        this.acao = acao;
    }

    public static AlbumFormView cadastro(AlbumDTO form) {
        return new AlbumFormView(
                null,
                valor(form.titulo()),
                valor(form.artista()),
                valor(form.anoLancamento()),
                false,
                "/albums"
        );
    }

    public static AlbumFormView edicao(Album album) {
        return new AlbumFormView(
                album.getId(),
                album.getTitulo(),
                album.getArtista(),
                String.valueOf(album.getAnoLancamento()),
                true,
                "/albums/" + album.getId()
        );
    }

    public static AlbumFormView edicao(String id, AlbumDTO form) {
        return new AlbumFormView(
                id,
                valor(form.titulo()),
                valor(form.artista()),
                valor(form.anoLancamento()),
                true,
                "/albums/" + id
            );
    }

    private static String valor(String valor) {
        return Objects.toString(valor, "");
    }

    public String getId() {
        return id;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getArtista() {
        return artista;
    }

    public String getAnoLancamento() {
        return anoLancamento;
    }

    public boolean isEdicao() {
        return edicao;
    }

    public String getAcao() {
        return acao;
    }
}
