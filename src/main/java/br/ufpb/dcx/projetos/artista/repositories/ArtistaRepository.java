package br.ufpb.dcx.projetos.artista.repositories;

import br.ufpb.dcx.projetos.artista.models.Artista;

import java.util.List;
import java.util.Optional;

public interface ArtistaRepository {

    void criar(Artista artista);

    boolean atualizar(Artista artista);

    Optional<Artista> buscarPorId(String id);

    List<Artista> listarTodos();

    List<Artista> buscar(String termo);

    boolean remover(String id);
}
