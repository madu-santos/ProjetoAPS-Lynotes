package br.ufpb.dcx.projetos.album;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock
    private AlbumRepository albumRepository;

    @InjectMocks
    private AlbumService albumService;

    private Album album1;
    private Album album2;

    @BeforeEach
    void setUp() {
        album1 = new Album("Album Teste 1", "Artista 1", 2000);
        album1.setId(1L);
        album2 = new Album("Album Teste 2", "Artista 2", 2005);
        album2.setId(2L);
    }

    @Test
    void whenSaveAlbum_shouldReturnAlbum() {
        when(albumRepository.save(any(Album.class))).thenReturn(album1);

        Album savedAlbum = albumService.save(new Album("Album Teste 1", "Artista 1", 2000));

        assertNotNull(savedAlbum);
        assertEquals(album1.getTitulo(), savedAlbum.getTitulo());
        verify(albumRepository, times(1)).save(any(Album.class));
    }

    @Test
    void whenFindAll_shouldReturnAllAlbums() {
        when(albumRepository.findAll()).thenReturn(Arrays.asList(album1, album2));

        List<Album> albums = albumService.findAll();

        assertNotNull(albums);
        assertEquals(2, albums.size());
        verify(albumRepository, times(1)).findAll();
    }

    @Test
    void whenFindById_shouldReturnAlbum() {
        when(albumRepository.findById(1L)).thenReturn(Optional.of(album1));

        Optional<Album> foundAlbum = albumService.findById(1L);

        assertTrue(foundAlbum.isPresent());
        assertEquals(album1.getTitulo(), foundAlbum.get().getTitulo());
        verify(albumRepository, times(1)).findById(1L);
    }

    @Test
    void whenFindById_shouldReturnEmptyOptional() {
        when(albumRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Album> foundAlbum = albumService.findById(99L);

        assertFalse(foundAlbum.isPresent());
        verify(albumRepository, times(1)).findById(99L);
    }

    @Test
    void whenUpdateAlbum_shouldReturnUpdatedAlbum() {
        Album updatedDetails = new Album("Album Atualizado", "Artista Atualizado", 2010);
        when(albumRepository.findById(1L)).thenReturn(Optional.of(album1));
        when(albumRepository.save(any(Album.class))).thenReturn(updatedDetails);

        Album result = albumService.update(1L, updatedDetails);

        assertNotNull(result);
        assertEquals(updatedDetails.getTitulo(), result.getTitulo());
        assertEquals(updatedDetails.getArtista(), result.getArtista());
        assertEquals(updatedDetails.getAnoLancamento(), result.getAnoLancamento());
        verify(albumRepository, times(1)).findById(1L);
        verify(albumRepository, times(1)).save(any(Album.class));
    }

    @Test
    void whenUpdateAlbumNotFound_shouldThrowException() {
        Album updatedDetails = new Album("Album Atualizado", "Artista Atualizado", 2010);
        when(albumRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            albumService.update(99L, updatedDetails);
        });

        assertEquals("Album not found with id 99", exception.getMessage());
        verify(albumRepository, times(1)).findById(99L);
        verify(albumRepository, never()).save(any(Album.class));
    }

    @Test
    void whenDeleteById_shouldCallRepositoryDelete() {
        doNothing().when(albumRepository).deleteById(1L);

        albumService.deleteById(1L);

        verify(albumRepository, times(1)).deleteById(1L);
    }
}