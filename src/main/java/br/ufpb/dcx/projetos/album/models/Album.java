package br.ufpb.dcx.projetos.album.models;

import java.util.Objects;
import java.util.UUID;

public final class Album {

    private final String id;
    private final String titulo;
    private final String artista;
    private final int anoLancamento;
    private final Double nota;
    private final String usuarioId;
    private final String youtubeId;
    private final String capaUrl;
    private final String resenha;

    public Album(String id, String titulo, String artista, int anoLancamento, Double nota, String usuarioId, String youtubeId, String capaUrl, String resenha) {
        this.id = Objects.requireNonNull(id);
        this.titulo = titulo;
        this.artista = artista;
        this.anoLancamento = anoLancamento;
        this.nota = nota;
        this.usuarioId = usuarioId;
        this.youtubeId = youtubeId;
        this.capaUrl = capaUrl;
        this.resenha = resenha;
    }

    public Album(String id, String titulo, String artista, int anoLancamento, Double nota, String usuarioId, String youtubeId, String capaUrl) {
        this(id, titulo, artista, anoLancamento, nota, usuarioId, youtubeId, capaUrl, null);
    }

    public Album(String id, String titulo, String artista, int anoLancamento, Double nota, String usuarioId) {
        this(id, titulo, artista, anoLancamento, nota, usuarioId, null, null, null);
    }

    public Album(String id, String titulo, String artista, int anoLancamento, String usuarioId) {
        this(id, titulo, artista, anoLancamento, null, usuarioId, null, null, null);
    }

    public static Album novo(String titulo, String artista, int anoLancamento, String usuarioId) {
        return new Album(UUID.randomUUID().toString(), titulo, artista, anoLancamento, null, usuarioId, null, null, null);
    }

    public static Album novo(String titulo, String artista, int anoLancamento, Double nota, String usuarioId) {
        return new Album(UUID.randomUUID().toString(), titulo, artista, anoLancamento, nota, usuarioId, null, null, null);
    }

    public static Album novo(String titulo, String artista, int anoLancamento, Double nota, String usuarioId, String youtubeId, String capaUrl, String resenha) {
        return new Album(UUID.randomUUID().toString(), titulo, artista, anoLancamento, nota, usuarioId, youtubeId, capaUrl, resenha);
    }

    public static Album novo(String titulo, String artista, int anoLancamento, Double nota, String usuarioId, String youtubeId, String capaUrl) {
        return new Album(UUID.randomUUID().toString(), titulo, artista, anoLancamento, nota, usuarioId, youtubeId, capaUrl, null);
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

    public int getAnoLancamento() {
        return anoLancamento;
    }

    public String getUsuarioId() {
        return usuarioId;
    }

    public Double getNota() {
        return nota;
    }

    public String getYoutubeId() {
        return youtubeId;
    }

    public String getCapaUrl() {
        return capaUrl;
    }

    public String getResenha() {
        return resenha;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Album album = (Album) o;
        return anoLancamento == album.anoLancamento && Objects.equals(id, album.id) && Objects.equals(titulo, album.titulo) && Objects.equals(artista, album.artista);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titulo, artista, anoLancamento);
    }

    @Override
    public String toString() {
        return "Album{" +
               "id='" + id + '\'' +
               ", titulo='" + titulo + '\'' +
               ", artista='" + artista + '\'' +
               ", anoLancamento=" + anoLancamento +
               ", usuarioId='" + usuarioId + '\'' +
               '}';
    }
}
