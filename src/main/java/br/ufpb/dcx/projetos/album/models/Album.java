package br.ufpb.dcx.projetos.album.models;

import java.util.Objects;
import java.util.UUID;

public final class Album {

    private final String id;
    private final String titulo;
    private final String artista;
    private final int anoLancamento;

    public Album(String id, String titulo, String artista, int anoLancamento) {
        this.id = Objects.requireNonNull(id);
        this.titulo = titulo;
        this.artista = artista;
        this.anoLancamento = anoLancamento;
    }

    public static Album novo(String titulo, String artista, int anoLancamento) {
        return new Album(UUID.randomUUID().toString(), titulo, artista, anoLancamento);
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
               '}';
    }
}
