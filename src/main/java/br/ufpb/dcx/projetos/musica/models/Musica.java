package br.ufpb.dcx.projetos.musica.models;

import java.util.Objects;
import java.util.UUID;

public final class Musica {

    private final String id;
    private final String titulo;
    private final String genero;
    private final int duracaoSegundos;
    private final String albumId; // Chave estrangeira para o Álbum (pode ser nulo)

    public Musica(String id, String titulo, String genero, int duracaoSegundos, String albumId) {
        this.id = Objects.requireNonNull(id);
        this.titulo = titulo;
        this.genero = genero;
        this.duracaoSegundos = duracaoSegundos;
        this.albumId = albumId;
    }

    public static Musica novo(String titulo, String genero, int duracaoSegundos, String albumId) {
        return new Musica(UUID.randomUUID().toString(), titulo, genero, duracaoSegundos, albumId);
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

    public int getDuracaoSegundos() {
        return duracaoSegundos;
    }

    public String getAlbumId() {
        return albumId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Musica musica = (Musica) o;
        return duracaoSegundos == musica.duracaoSegundos && Objects.equals(id, musica.id) && Objects.equals(titulo, musica.titulo) && Objects.equals(genero, musica.genero) && Objects.equals(albumId, musica.albumId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titulo, genero, duracaoSegundos, albumId);
    }

    @Override
    public String toString() {
        return "Musica{" +
               "id='" + id + '\'' +
               ", titulo='" + titulo + '\'' +
               ", genero='" + genero + '\'' +
               ", duracaoSegundos=" + duracaoSegundos +
               ", albumId='" + albumId + '\'' +
               '}';
    }
}
