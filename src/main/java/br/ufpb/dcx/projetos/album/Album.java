package br.ufpb.dcx.projetos.album;

import br.ufpb.dcx.projetos.musica.Musica;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Range;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "O título é obrigatório")
    private String titulo;

    @NotBlank(message = "O artista é obrigatório")
    private String artista;

    @NotNull(message = "O ano de lançamento é obrigatório")
    @Min(value = 1900, message = "O ano de lançamento deve ser a partir de 1900")
    @Range(min = 1900, max = 2100, message = "O ano de lançamento deve estar entre 1900 e 2100") // Added a max for realism
    private int anoLancamento;

    @OneToMany(mappedBy = "album", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Musica> musicas = new ArrayList<>();

    public Album() {
    }

    public Album(String titulo, String artista, int anoLancamento) {
        this.titulo = titulo;
        this.artista = artista;
        this.anoLancamento = anoLancamento;
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

    public String getArtista() {
        return artista;
    }

    public void setArtista(String artista) {
        this.artista = artista;
    }

    public int getAnoLancamento() {
        return anoLancamento;
    }

    public void setAnoLancamento(int anoLancamento) {
        this.anoLancamento = anoLancamento;
    }

    public List<Musica> getMusicas() {
        return musicas;
    }

    public void setMusicas(List<Musica> musicas) {
        this.musicas = musicas;
    }

    // Helper method to add music to the album and set the album on the music side
    public void addMusica(Musica musica) {
        musicas.add(musica);
        musica.setAlbum(this);
    }

    // Helper method to remove music from the album and unset the album on the music side
    public void removeMusica(Musica musica) {
        musicas.remove(musica);
        musica.setAlbum(null);
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
               "id=" + id +
               ", titulo='" + titulo + '\'' +
               ", artista='" + artista + '\'' +
               ", anoLancamento=" + anoLancamento +
               ", musicas=" + musicas.size() +
               '}';
    }
}