package br.ufpb.dcx.projetos;

import br.ufpb.dcx.projetos.album.Album;
import br.ufpb.dcx.projetos.album.AlbumRepository;
import br.ufpb.dcx.projetos.musica.Musica;
import br.ufpb.dcx.projetos.musica.MusicaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AlbumMusicaIntegrationTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private MusicaRepository musicaRepository;

    @Test
    void whenAlbumIsDeleted_thenAssociatedMusicasShouldAlsoBeDeleted() {
        // Given
        Album album = new Album("Album Teste Integracao", "Artista Integracao", 2023);
        entityManager.persistAndFlush(album);

        Musica musica1 = new Musica("Musica A", "Rock", 200, album);
        Musica musica2 = new Musica("Musica B", "Pop", 180, album);
        entityManager.persistAndFlush(musica1);
        entityManager.persistAndFlush(musica2);

        // Verify that album and musicas are saved
        assertThat(albumRepository.findById(album.getId())).isPresent();
        assertThat(musicaRepository.findByAlbum(album)).hasSize(2);

        // When
        albumRepository.deleteById(album.getId());
        entityManager.flush(); // Ensure deletion is propagated to the database

        // Then
        assertThat(albumRepository.findById(album.getId())).isNotPresent();
        assertThat(musicaRepository.findByAlbum(album)).isEmpty();
    }

    @Test
    void whenMusicasAreAssociatedWithAlbum_theyShouldBePersistedCorrectly() {
        // Given
        Album album = new Album("Album Com Musicas", "Banda Legal", 2022);
        entityManager.persistAndFlush(album);

        Musica musica1 = new Musica("Musica X", "Jazz", 250, album);
        Musica musica2 = new Musica("Musica Y", "Blues", 300, album);
        entityManager.persistAndFlush(musica1);
        entityManager.persistAndFlush(musica2);

        // When
        Optional<Album> foundAlbum = albumRepository.findById(album.getId());
        List<Musica> foundMusicas = musicaRepository.findByAlbum(album);

        // Then
        assertThat(foundAlbum).isPresent();
        assertThat(foundAlbum.get().getTitulo()).isEqualTo("Album Com Musicas");
        assertThat(foundMusicas).hasSize(2);
        assertThat(foundMusicas).extracting(Musica::getTitulo).containsExactlyInAnyOrder("Musica X", "Musica Y");
        assertThat(foundMusicas).allMatch(m -> m.getAlbum().getId().equals(album.getId()));
    }
}