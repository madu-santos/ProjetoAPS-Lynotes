package br.ufpb.dcx.projetos.musica.views;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.musica.dto.MusicaDTO;
import br.ufpb.dcx.projetos.musica.models.Musica;

import java.util.List;
import java.util.Objects;

public final class MusicaDTOView {

    private final String id;
    private final String titulo;
    private final String artista;
    private final String genero;
    private final String duracaoSegundos;
    private final String resenha;
    private final String nota;
    private final String spotifyUrl;
    private final String youtubeUrl;
    private final String albumId;
    private final String playlistId;
    private final String novaPlaylistNome;
    private final String youtubeId;
    private final String capaUrl;
    private final boolean edicao;
    private final String acao;
    private final List<Album> albums;
    private final List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists;

    private MusicaDTOView(
            String id,
            String titulo,
            String artista,
            String genero,
            String duracaoSegundos,
            String resenha,
            String nota,
            String spotifyUrl,
            String youtubeUrl,
            String albumId,
            String playlistId,
            String novaPlaylistNome,
            String youtubeId,
            String capaUrl,
            boolean edicao,
            String acao,
            List<Album> albums,
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists
    ) {
        this.id = id;
        this.titulo = titulo;
        this.artista = artista;
        this.genero = genero;
        this.duracaoSegundos = duracaoSegundos;
        this.resenha = resenha;
        this.nota = nota;
        this.spotifyUrl = spotifyUrl;
        this.youtubeUrl = youtubeUrl;
        this.albumId = albumId;
        this.playlistId = playlistId;
        this.novaPlaylistNome = novaPlaylistNome;
        this.youtubeId = youtubeId;
        this.capaUrl = capaUrl;
        this.edicao = edicao;
        this.acao = acao;
        this.albums = albums;
        this.playlists = playlists;
    }

    public static MusicaDTOView cadastro(MusicaDTO form, List<Album> albums) {
        return new MusicaDTOView(
                null,
                valor(form.titulo()),
                valor(form.artista()),
                null, // genero
                valor(form.duracaoSegundos()),
                valor(form.resenha()),
                valor(form.nota()),
                valor(form.spotifyUrl()),
                valor(form.youtubeUrl()),
                valor(form.albumId()),
                valor(form.playlistId()),
                valor(form.novaPlaylistNome()),
                valor(form.youtubeId()),
                valor(form.capaUrl()),
                false,
                "/musicas",
                albums,
                java.util.Collections.emptyList()
        );
    }

    public static MusicaDTOView cadastro(MusicaDTO form, List<Album> albums, List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists) {
        return new MusicaDTOView(
                null,
                valor(form.titulo()),
                valor(form.artista()),
                null, // genero
                valor(form.duracaoSegundos()),
                valor(form.resenha()),
                valor(form.nota()),
                valor(form.spotifyUrl()),
                valor(form.youtubeUrl()),
                valor(form.albumId()),
                valor(form.playlistId()),
                valor(form.novaPlaylistNome()),
                valor(form.youtubeId()),
                valor(form.capaUrl()),
                false,
                "/musicas",
                albums,
                playlists
        );
    }

    public static MusicaDTOView edicao(Musica musica, List<Album> albums, List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists) {
        return new MusicaDTOView(
                musica.getId(),
                musica.getTitulo(),
                musica.getArtista(),
                musica.getGenero(),
                musica.getDuracaoSegundos() != null ? String.valueOf(musica.getDuracaoSegundos()) : "",
                musica.getResenha(),
                musica.getNota() != null ? String.valueOf(musica.getNota()) : "",
                musica.getSpotifyUrl(),
                musica.getYoutubeUrl(),
                musica.getAlbumId(),
                null,
                null,
                musica.getYoutubeId(),
                musica.getCapaUrl(),
                true,
                "/musicas/edit/" + musica.getId(),
                albums,
                playlists
        );
    }

    public static MusicaDTOView edicao(String id, MusicaDTO form, List<Album> albums, List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists) {
        return new MusicaDTOView(
                id,
                valor(form.titulo()),
                valor(form.artista()),
                null, // genero
                valor(form.duracaoSegundos()),
                valor(form.resenha()),
                valor(form.nota()),
                valor(form.spotifyUrl()),
                valor(form.youtubeUrl()),
                valor(form.albumId()),
                valor(form.playlistId()),
                valor(form.novaPlaylistNome()),
                valor(form.youtubeId()),
                valor(form.capaUrl()),
                true,
                "/musicas/edit/" + id,
                albums,
                playlists
        );
    }

    private static String valor(String valor) {
        return Objects.toString(valor, "");
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public String getPlaylistId() {
        return playlistId;
    }

    public String getNovaPlaylistNome() {
        return novaPlaylistNome;
    }

    public List<br.ufpb.dcx.projetos.playlist.models.Playlist> getPlaylists() {
        return playlists;
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

    public String getYoutubeId() {
        return youtubeId;
    }

    public String getCapaUrl() {
        return capaUrl;
    }
}
