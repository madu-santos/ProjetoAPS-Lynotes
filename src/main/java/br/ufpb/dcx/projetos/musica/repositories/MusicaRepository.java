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

    List<Musica> buscarPorUsuarioId(String usuarioId);

    List<Musica> buscar(String termo, String usuarioId);

    default Optional<Musica> buscarPorYouTubeId(String youtubeId) {
        return Optional.empty();
    }

    default boolean alternarOculta(String id, String usuarioId) {
        return false;
    }

    boolean remover(String id);
}
