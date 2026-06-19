package br.ufpb.dcx.projetos.musica.repositories;

import br.ufpb.dcx.projetos.musica.models.Musica;

import java.util.List;
import java.util.Optional;

public interface MusicaRepository {

    void criar(Musica musica);

    boolean atualizar(Musica musica);

    Optional<Musica> buscarPorId(String id);

    List<Musica> listarTodas();

    List<Musica> buscarPorAlbumId(String albumId);

    boolean remover(String id);
}
