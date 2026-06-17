package br.ufpb.dcx.projetos.musica;

import br.ufpb.dcx.projetos.album.Album;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Objects;

@Entity
public class Musica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O título da música é obrigatório")
    private String titulo;

    @NotBlank(message = "O gênero da música é obrigatório")
    private String genero;

    @NotNull(message = "A duração da música é obrigatória")
    @Min(value = 1, message = "A duração da música deve ser de pelo menos 1 segundo")
    private int duracaoSegundos;

    @ManyToOne
    @JoinColumn(name = "album_id")
    private Album album; // Album pode ser nulo se a música não estiver associada a um álbum

    public Musica() {
    }

    public Musica(String titulo, String genero, int duracaoSegundos, Album album) {
        this.titulo = titulo;
        this.genero = genero;
        this.duracaoSegundos = duracaoSegundos;
        this.album = album;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getGenero() {
        return genero;
    }

    public void setGenero(String genero) {
        this.genero = genero;
    }

    public int getDuracaoSegundos() {
        return duracaoSegundos;
    }

    public void setDuracaoSegundos(int duracaoSegundos) {
        this.duracaoSegundos = duracaoSegundos;
    }

    public Album getAlbum() {
        return album;
    }

    public void setAlbum(Album album) {
        this.album = album;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Musica musica = (Musica) o;
        return duracaoSegundos == musica.duracaoSegundos && Objects.equals(id, musica.id) && Objects.equals(titulo, musica.titulo) && Objects.equals(genero, musica.genero) && Objects.equals(album, musica.album);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, titulo, genero, duracaoSegundos, album);
    }

    @Override
    public String toString() {
        return "Musica{" +
               "id=" + id +
               ", titulo='" + titulo + '\'' +
               ", genero='" + genero + '\'' +
               ", duracaoSegundos=" + duracaoSegundos +
               ", album=" + (album != null ? album.getTitulo() : "N/A") +
               '}';
    }
}