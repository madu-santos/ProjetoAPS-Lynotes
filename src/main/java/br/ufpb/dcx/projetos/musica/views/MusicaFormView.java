package br.ufpb.dcx.projetos.musica.views;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.musica.dto.MusicaForm;
import br.ufpb.dcx.projetos.musica.models.Musica;

import java.util.List;
import java.util.Objects;

public final class MusicaFormView {

    private final String id;
    private final String titulo;
    private final String genero;
    private final String duracaoSegundos;
    private final String albumId;
    private final boolean edicao;
    private final String acao;
    private final List<Album> albums;

    private MusicaFormView(
            String id,
            String titulo,
            String genero,
            String duracaoSegundos,
            String albumId,
            boolean edicao,
            String acao,
            List<Album> albums
    ) {
        this.id = id;
        this.titulo = titulo;
        this.genero = genero;
        this.duracaoSegundos = duracaoSegundos;
        this.albumId = albumId;
        this.edicao = edicao;
        this.acao = acao;
        this.albums = albums;
    }

    public static MusicaFormView cadastro(MusicaForm form, List<Album> albums) {
        return new MusicaFormView(
                null,
                valor(form.titulo()),
                valor(form.genero()),
                valor(form.duracaoSegundos()),
                valor(form.albumId()),
                false,
                "/musicas",
                albums
        );
    }

    public static MusicaFormView edicao(Musica musica, List<Album> albums) {
        return new MusicaFormView(
                musica.getId(),
                musica.getTitulo(),
                musica.getGenero(),
                String.valueOf(musica.getDuracaoSegundos()),
                musica.getAlbumId(),
                true,
                "/musicas/" + musica.getId(),
                albums
        );
    }

    public static MusicaFormView edicao(String id, MusicaForm form, List<Album> albums) {
        return new MusicaFormView(
                id,
                valor(form.titulo()),
                valor(form.genero()),
                valor(form.duracaoSegundos()),
                valor(form.albumId()),
                true,
                "/musicas/" + id,
                albums
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

    public String getGenero() {
        return genero;
    }

    public String getDuracaoSegundos() {
        return duracaoSegundos;
    }

    public String getAlbumId() {
        return albumId;
    }

    public boolean isEdicao() {
        return edicao;
    }

    public String getAcao() {
        return acao;
    }

    public List<Album> getAlbums() {
        return albums;
    }
}
