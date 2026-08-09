package br.ufpb.dcx.projetos.playlist.repositories;

import br.ufpb.dcx.projetos.playlist.models.Playlist;
import br.ufpb.dcx.projetos.playlist.models.PlaylistItem;

import java.util.List;
import java.util.Optional;

public interface PlaylistRepository {

    void create(Playlist playlist);

    Optional<Playlist> findById(String id);

    List<Playlist> findByUsuarioId(String usuarioId);

    List<Playlist> buscar(String termo, String usuarioId);

    void update(Playlist playlist);

    boolean delete(String playlistId);

    int countItemsByPlaylistId(String playlistId);

    void createItem(PlaylistItem item);

    List<PlaylistItem> findItemsByPlaylistId(String playlistId);

    List<PlaylistItem> findItemsByPlaylistIds(List<String> playlistIds);

    Optional<PlaylistItem> findItemById(String itemId);

    boolean deleteItem(String itemId);

    boolean alternarOcultaItem(String itemId);
}
