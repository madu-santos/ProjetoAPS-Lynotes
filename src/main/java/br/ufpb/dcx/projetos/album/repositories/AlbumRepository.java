package br.ufpb.dcx.projetos.album.repositories;

import br.ufpb.dcx.projetos.album.models.Album;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository {

    void criar(Album album);

    boolean atualizar(Album album);

    Optional<Album> buscarPorId(String id, String usuarioId);

    List<Album> listarTodos(String usuarioId);
    
    List<Album> listarTodosGlobal();

    List<Album> buscar(String termo, String usuarioId);

    default Optional<Album> buscarPorYouTubeId(String youtubeId) {
        return Optional.empty();
    }

    boolean remover(String id, String usuarioId);
}
