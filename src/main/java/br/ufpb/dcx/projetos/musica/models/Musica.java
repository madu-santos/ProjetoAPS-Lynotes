package br.ufpb.dcx.projetos.musica.models;

import java.util.Objects;
import java.util.UUID;

public final class Musica {

    private final String id;
    private final String titulo;
    private final String artista;
    private final String genero;
    private final Integer duracaoSegundos; // Nullable
    private final String resenha; // Nullable
    private final Double nota; // Nullable (1 a 5 estrelas)
    private final String spotifyUrl; // Nullable
    private final String youtubeUrl; // Nullable
    private final String albumId; // Nullable
    private final String usuarioId; // Nullable

    public Musica(String id, String titulo, String artista, String genero, Integer duracaoSegundos, String resenha, Double nota, String spotifyUrl, String youtubeUrl, String albumId, String usuarioId) {
        this.id = Objects.requireNonNull(id);
        this.titulo = Objects.requireNonNull(titulo);
        this.artista = Objects.requireNonNull(artista);
        this.genero = genero;
        this.duracaoSegundos = duracaoSegundos;
        this.resenha = resenha;
        this.nota = nota;
        this.spotifyUrl = spotifyUrl;
        this.youtubeUrl = youtubeUrl;
        this.albumId = albumId;
        this.usuarioId = usuarioId;
    }

    public static Musica novo(String titulo, String artista, String genero, Integer duracaoSegundos, String resenha, Double nota, String spotifyUrl, String youtubeUrl, String albumId, String usuarioId) {
        return new Musica(UUID.randomUUID().toString(), titulo, artista, genero, duracaoSegundos, resenha, nota, spotifyUrl, youtubeUrl, albumId, usuarioId);
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

    public Integer getDuracaoSegundos() {
        return duracaoSegundos;
    }

    public String getResenha() {
        return resenha;
    }

    public Double getNota() {
        return nota;
    }

    public String getSpotifyUrl() {
        return spotifyUrl;
    }

    public String getYoutubeUrl() {
        return youtubeUrl;
    }

    public String getAlbumId() {
        return albumId;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Musica musica = (Musica) o;
        return Objects.equals(id, musica.id) && Objects.equals(titulo, musica.titulo) && Objects.equals(artista, musica.artista) && Objects.equals(genero, musica.genero) && Objects.equals(duracaoSegundos, musica.duracaoSegundos) && Objects.equals(resenha, musica.resenha) && Objects.equals(nota, musica.nota) && Objects.equals(spotifyUrl, musica.spotifyUrl) && Objects.equals(albumId, musica.albumId) && Objects.equals(usuarioId, musica.usuarioId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titulo, artista, genero, duracaoSegundos, resenha, nota, spotifyUrl, albumId, usuarioId);
    }

    @Override
    public String toString() {
        return "Musica{" +
               "id='" + id + '\'' +
               ", titulo='" + titulo + '\'' +
               ", artista='" + artista + '\'' +
               ", genero='" + genero + '\'' +
               ", duracaoSegundos=" + duracaoSegundos +
               ", resenha='" + resenha + '\'' +
               ", nota=" + nota +
               ", spotifyUrl='" + spotifyUrl + '\'' +
               ", albumId='" + albumId + '\'' +
               ", usuarioId='" + usuarioId + '\'' +
               '}';
    }
}
