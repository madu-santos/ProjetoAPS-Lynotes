package br.ufpb.dcx.projetos.playlist.services;

import br.ufpb.dcx.projetos.playlist.models.Playlist;
import br.ufpb.dcx.projetos.playlist.models.PlaylistItem;
import br.ufpb.dcx.projetos.playlist.repositories.PlaylistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class PlaylistServiceTest {

    @Mock
    private PlaylistRepository playlistRepository;

    @InjectMocks
    private PlaylistService playlistService;

    @Test
    void deveCriarPlaylistComSucesso() {
        String nome = "Minhas Músicas";
        String usuarioId = "user123";

        Playlist playlist = playlistService.createPlaylist(nome, usuarioId);

        assertNotNull(playlist);
        assertEquals(nome, playlist.getNome());
        assertEquals(usuarioId, playlist.getUsuarioId());
        verify(playlistRepository, times(1)).create(any(Playlist.class));
    }

    @Test
    void naoDeveCriarPlaylistSemNome() {
        assertThrows(IllegalArgumentException.class, () -> playlistService.createPlaylist("", "user123"));
    }

    @Test
    void deveAtualizarPlaylist() {
        Playlist playlist = Playlist.novo("Antigo", "user123", false);
        when(playlistRepository.findById(playlist.getId())).thenReturn(Optional.of(playlist));

        playlistService.updatePlaylist(playlist.getId(), "Novo", true, "user123");

        ArgumentCaptor<Playlist> captor = ArgumentCaptor.forClass(Playlist.class);
        verify(playlistRepository, times(1)).update(captor.capture());
        
        Playlist atualizada = captor.getValue();
        assertEquals("Novo", atualizada.getNome());
        assertTrue(atualizada.isOculta());
    }

    @Test
    void naoDeveAtualizarPlaylistDeOutroUsuario() {
        Playlist playlist = Playlist.novo("Antigo", "user123", false);
        when(playlistRepository.findById(playlist.getId())).thenReturn(Optional.of(playlist));

        assertThrows(IllegalArgumentException.class, () -> {
            playlistService.updatePlaylist(playlist.getId(), "Novo", true, "outroUser");
        });
    }

    @Test
    void deveExcluirPlaylist() {
        Playlist playlist = Playlist.novo("Para Excluir", "user123", false);
        when(playlistRepository.findById(playlist.getId())).thenReturn(Optional.of(playlist));

        playlistService.deletePlaylist(playlist.getId(), "user123");

        verify(playlistRepository, times(1)).delete(playlist.getId());
    }

    @Test
    void naoDeveExcluirPlaylistOcultaPrincipal() {
        Playlist playlist = Playlist.novo("Músicas Catalogadas", "user123", true);
        when(playlistRepository.findById(playlist.getId())).thenReturn(Optional.of(playlist));

        assertThrows(IllegalArgumentException.class, () -> {
            playlistService.deletePlaylist(playlist.getId(), "user123");
        });
    }
}
