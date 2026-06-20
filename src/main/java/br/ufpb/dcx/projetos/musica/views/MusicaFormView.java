package br.ufpb.dcx.projetos.musica.views;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.musica.dto.MusicaForm;
import br.ufpb.dcx.projetos.musica.models.Musica;

import java.util.List;
import java.util.Objects;

public final class MusicaFormView {

    private final String id;
    private final String titulo;
    private final String artista;
    private final String genero;
    private final String duracaoSegundos;
    private final String resenha;
    private final String nota;
    private final String spotifyUrl;
    private final String albumId;
    private final boolean edicao;
    private final String acao;
    private final List<Album> albums;

    private MusicaFormView(
            String id,
            String titulo,
            String artista,
            String genero,
            String duracaoSegundos,
            String resenha,
            String nota,
            String spotifyUrl,
            String albumId,
            boolean edicao,
            String acao,
            List<Album> albums
    ) {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.genero = genero;
        this.duracaoSegundos = duracaoSegundos;
        this.resenha = resenha;
        this.nota = nota;
        this.spotifyUrl = spotifyUrl;
        this.albumId = albumId;
        this.edicao = edicao;
        this.acao = acao;
        this.albums = albums;
    }

    public static MusicaFormView cadastro(MusicaForm form, List<Album> albums) {
        return new MusicaFormView(
                null,
                valor(form.titulo()),
                valor(form.artista()),
                null, // genero
                valor(form.duracaoSegundos()),
                valor(form.resenha()),
                valor(form.nota()),
                valor(form.spotifyUrl()),
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
                musica.getArtista(),
                musica.getGenero(),
                musica.getDuracaoSegundos() != null ? String.valueOf(musica.getDuracaoSegundos()) : "",
                musica.getResenha(),
                musica.getNota() != null ? String.valueOf(musica.getNota()) : "",
                musica.getSpotifyUrl(),
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
                valor(form.artista()),
                null, // genero
                valor(form.duracaoSegundos()),
                valor(form.resenha()),
                valor(form.nota()),
                valor(form.spotifyUrl()),
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

    public String getArtista() {
        return artista;
    }

    public String getGenero() {
        return genero;
    }

    public String getDuracaoSegundos() {
        return duracaoSegundos;
    }

    public String getResenha() {
        return resenha;
    }

    public String getNota() {
        return nota;
    }

    public String getSpotifyUrl() {
        return spotifyUrl;
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
