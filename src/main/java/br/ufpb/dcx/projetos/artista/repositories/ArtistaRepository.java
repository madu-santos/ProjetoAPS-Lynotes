package br.ufpb.dcx.projetos.artista.repositories;

import br.ufpb.dcx.projetos.artista.models.Artista;

import java.util.List;
import java.util.Optional;

public interface ArtistaRepository {

    void criar(Artista artista);

    boolean atualizar(Artista artista);

    Optional<Artista> buscarPorId(String id, String usuarioId);

    List<Artista> listarTodos(String usuarioId);
    
    List<Artista> listarTodosGlobal();

    List<Artista> buscar(String termo, String usuarioId);

    default Optional<Artista> buscarPorYouTubeId(String youtubeId) {
        return Optional.empty();
    }

    List<Artista> listarPendentes();

    boolean atualizarStatusVerificacao(String id, br.ufpb.dcx.projetos.artista.models.StatusVerificacao status);

    boolean remover(String id, String usuarioId);
}
