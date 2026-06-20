package br.ufpb.dcx.projetos.album.repositories;

import br.ufpb.dcx.projetos.album.models.Album;

import java.util.List;
import java.util.Optional;

public interface AlbumRepository {

    void criar(Album album);

    boolean atualizar(Album album);

    Optional<Album> buscarPorId(String id);

    List<Album> listarTodos();

    boolean remover(String id);
}
