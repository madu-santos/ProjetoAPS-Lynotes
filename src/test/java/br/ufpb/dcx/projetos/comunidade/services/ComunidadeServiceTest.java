package br.ufpb.dcx.projetos.comunidade.services;

import br.ufpb.dcx.projetos.comunidade.models.Post;
import br.ufpb.dcx.projetos.comunidade.repositories.ComunidadeDbRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComunidadeServiceTest {

    @Mock
    private ComunidadeDbRepository comunidadeRepository;

    @InjectMocks
    private ComunidadeService comunidadeService;

    @Test
    void deveCriarPostComSucesso() {
        comunidadeService.criarPost("user123", "Meu primeiro post!");
        verify(comunidadeRepository, times(1)).salvarPost(any(Post.class));
    }

    @Test
    void naoDeveCriarPostVazio() {
        assertThrows(IllegalArgumentException.class, () -> comunidadeService.criarPost("user123", "   "));
    }

    @Test
    void naoDeveCriarPostMuitoLongo() {
        String textoLongo = "a".repeat(501);
        assertThrows(IllegalArgumentException.class, () -> comunidadeService.criarPost("user123", textoLongo));
    }

    @Test
    void deveEditarPostComSucesso() {
        Post post = new Post("user123", "Texto antigo");
        when(comunidadeRepository.buscarPostPorId(post.getId())).thenReturn(Optional.of(post));

        comunidadeService.editarPost(post.getId(), "user123", "Texto novo");

        verify(comunidadeRepository, times(1)).salvarPost(any(Post.class));
    }

    @Test
    void naoDeveEditarPostDeOutroUsuario() {
        Post post = new Post("user123", "Texto antigo");
        when(comunidadeRepository.buscarPostPorId(post.getId())).thenReturn(Optional.of(post));

        assertThrows(IllegalArgumentException.class, () -> {
            comunidadeService.editarPost(post.getId(), "outroUser", "Texto novo");
        });
    }

    @Test
    void deveCriarPostCompartilhadoComSucesso() {
        comunidadeService.criarPostCompartilhado("user123", "SHARE_MUSIC", "music1", "Recomendo!");
        verify(comunidadeRepository, times(1)).salvarPost(any(Post.class));
    }

    @Test
    void naoDeveCriarPostCompartilhadoComTipoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> {
            comunidadeService.criarPostCompartilhado("user123", "TIPO_INVALIDO", "item1", "Recomendo!");
        });
    }

    @Test
    void deveSeguirUsuarioComSucesso() {
        comunidadeService.seguir("user1", "user2");
        verify(comunidadeRepository, times(1)).seguir("user1", "user2");
    }

    @Test
    void naoDeveSeguirASiMesmo() {
        assertThrows(IllegalArgumentException.class, () -> {
            comunidadeService.seguir("user1", "user1");
        });
    }
}
