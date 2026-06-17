package br.ufpb.dcx.projetos.musica;

import br.ufpb.dcx.projetos.album.Album;
import br.ufpb.dcx.projetos.album.AlbumRepository;
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
class MusicaServiceTest {

    @Mock
    private MusicaRepository musicaRepository;

    @Mock
    private AlbumRepository albumRepository;

    @InjectMocks
    private MusicaService musicaService;

    private Musica musica1;
    private Musica musica2;
    private Album album;

    @BeforeEach
    void setUp() {
        album = new Album("Album Teste", "Artista Teste", 2020);
        album.setId(1L);

        musica1 = new Musica("Musica Teste 1", "Pop", 180, album);
        musica1.setId(1L);
        musica2 = new Musica("Musica Teste 2", "Rock", 240, null);
        musica2.setId(2L);
    }

    @Test
    void whenSaveMusicaWithAlbum_shouldReturnMusica() {
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(musicaRepository.save(any(Musica.class))).thenReturn(musica1);

        Musica savedMusica = musicaService.save(new Musica("Musica Teste 1", "Pop", 180, album));

        assertNotNull(savedMusica);
        assertEquals(musica1.getTitulo(), savedMusica.getTitulo());
        assertNotNull(savedMusica.getAlbum());
        assertEquals(album.getId(), savedMusica.getAlbum().getId());
        verify(albumRepository, times(1)).findById(album.getId());
        verify(musicaRepository, times(1)).save(any(Musica.class));
    }

    @Test
    void whenSaveMusicaWithoutAlbum_shouldReturnMusica() {
        Musica newMusica = new Musica("Musica Teste 3", "Jazz", 200, null);
        when(musicaRepository.save(any(Musica.class))).thenReturn(newMusica);

        Musica savedMusica = musicaService.save(newMusica);

        assertNotNull(savedMusica);
        assertEquals(newMusica.getTitulo(), savedMusica.getTitulo());
        assertNull(savedMusica.getAlbum());
        verify(albumRepository, never()).findById(anyLong());
        verify(musicaRepository, times(1)).save(any(Musica.class));
    }

    @Test
    void whenFindAll_shouldReturnAllMusicas() {
        when(musicaRepository.findAll()).thenReturn(Arrays.asList(musica1, musica2));

        List<Musica> musicas = musicaService.findAll();

        assertNotNull(musicas);
        assertEquals(2, musicas.size());
        verify(musicaRepository, times(1)).findAll();
    }

    @Test
    void whenFindById_shouldReturnMusica() {
        when(musicaRepository.findById(1L)).thenReturn(Optional.of(musica1));

        Optional<Musica> foundMusica = musicaService.findById(1L);

        assertTrue(foundMusica.isPresent());
        assertEquals(musica1.getTitulo(), foundMusica.get().getTitulo());
        verify(musicaRepository, times(1)).findById(1L);
    }

    @Test
    void whenFindById_shouldReturnEmptyOptional() {
        when(musicaRepository.findById(99L)).thenReturn(Optional.empty());

        Optional<Musica> foundMusica = musicaService.findById(99L);

        assertFalse(foundMusica.isPresent());
        verify(musicaRepository, times(1)).findById(99L);
    }

    @Test
    void whenUpdateMusica_shouldReturnUpdatedMusica() {
        Musica updatedDetails = new Musica("Musica Atualizada", "Eletronica", 210, album);
        when(musicaRepository.findById(1L)).thenReturn(Optional.of(musica1));
        when(albumRepository.findById(album.getId())).thenReturn(Optional.of(album));
        when(musicaRepository.save(any(Musica.class))).thenReturn(updatedDetails);

        Musica result = musicaService.update(1L, updatedDetails);

        assertNotNull(result);
        assertEquals(updatedDetails.getTitulo(), result.getTitulo());
        assertEquals(updatedDetails.getGenero(), result.getGenero());
        assertEquals(updatedDetails.getDuracaoSegundos(), result.getDuracaoSegundos());
        assertNotNull(result.getAlbum());
        assertEquals(album.getId(), result.getAlbum().getId());
        verify(musicaRepository, times(1)).findById(1L);
        verify(albumRepository, times(1)).findById(album.getId());
        verify(musicaRepository, times(1)).save(any(Musica.class));
    }

    @Test
    void whenUpdateMusicaToDisassociateAlbum_shouldReturnUpdatedMusica() {
        Musica updatedDetails = new Musica("Musica Atualizada", "Eletronica", 210, null);
        when(musicaRepository.findById(1L)).thenReturn(Optional.of(musica1));
        when(musicaRepository.save(any(Musica.class))).thenReturn(updatedDetails);

        Musica result = musicaService.update(1L, updatedDetails);

        assertNotNull(result);
        assertNull(result.getAlbum());
        verify(musicaRepository, times(1)).findById(1L);
        verify(albumRepository, never()).findById(anyLong());
        verify(musicaRepository, times(1)).save(any(Musica.class));
    }

    @Test
    void whenUpdateMusicaNotFound_shouldThrowException() {
        Musica updatedDetails = new Musica("Musica Atualizada", "Eletronica", 210, album);
        when(musicaRepository.findById(99L)).thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            musicaService.update(99L, updatedDetails);
        });

        assertEquals("Musica not found with id 99", exception.getMessage());
        verify(musicaRepository, times(1)).findById(99L);
        verify(albumRepository, never()).findById(anyLong());
        verify(musicaRepository, never()).save(any(Musica.class));
    }

    @Test
    void whenDeleteById_shouldCallRepositoryDelete() {
        doNothing().when(musicaRepository).deleteById(1L);

        musicaService.deleteById(1L);

        verify(musicaRepository, times(1)).deleteById(1L);
    }
}