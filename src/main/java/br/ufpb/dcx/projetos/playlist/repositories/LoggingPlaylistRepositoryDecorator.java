package br.ufpb.dcx.projetos.playlist.repositories;

import br.ufpb.dcx.projetos.playlist.models.Playlist;
import br.ufpb.dcx.projetos.playlist.models.PlaylistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class LoggingPlaylistRepositoryDecorator implements PlaylistRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingPlaylistRepositoryDecorator.class);
    private final PlaylistRepository decorado;

    public LoggingPlaylistRepositoryDecorator(PlaylistRepository decorado) {
        this.decorado = decorado;
    }

    @Override
    public void create(Playlist playlist) {
        long inicio = System.currentTimeMillis();
        decorado.create(playlist);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): create playlist executado em {}ms. id={} nome={}", (fim - inicio), playlist.getId(), playlist.getNome());
    }

    @Override
    public Optional<Playlist> findById(String id) {
        long inicio = System.currentTimeMillis();
        Optional<Playlist> resultado = decorado.findById(id);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: findById playlist executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public List<Playlist> findByUsuarioId(String usuarioId) {
        long inicio = System.currentTimeMillis();
        List<Playlist> resultado = decorado.findByUsuarioId(usuarioId);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): findByUsuarioId playlist executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public List<Playlist> buscar(String termo, String usuarioId) {
        long inicio = System.currentTimeMillis();
        List<Playlist> resultado = decorado.buscar(termo, usuarioId);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: buscar playlist executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public void update(Playlist playlist) {
        long inicio = System.currentTimeMillis();
        decorado.update(playlist);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): update playlist executado em {}ms. id={} nome={}", (fim - inicio), playlist.getId(), playlist.getNome());
    }

    @Override
    public boolean delete(String playlistId) {
        long inicio = System.currentTimeMillis();
        boolean resultado = decorado.delete(playlistId);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): delete playlist executado em {}ms. id={} resultado={}", (fim - inicio), playlistId, resultado);
        return resultado;
    }

    @Override
    public int countItemsByPlaylistId(String playlistId) {
        long inicio = System.currentTimeMillis();
        int resultado = decorado.countItemsByPlaylistId(playlistId);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: countItemsByPlaylistId playlist executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public void createItem(PlaylistItem item) {
        long inicio = System.currentTimeMillis();
        decorado.createItem(item);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): createItem playlist executado em {}ms. itemId={} playlistId={}", (fim - inicio), item.getId(), item.getPlaylistId());
    }

    @Override
    public List<PlaylistItem> findItemsByPlaylistId(String playlistId) {
        long inicio = System.currentTimeMillis();
        List<PlaylistItem> resultado = decorado.findItemsByPlaylistId(playlistId);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): findItemsByPlaylistId playlist executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public List<PlaylistItem> findItemsByPlaylistIds(List<String> playlistIds) {
        long inicio = System.currentTimeMillis();
        List<PlaylistItem> resultado = decorado.findItemsByPlaylistIds(playlistIds);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): findItemsByPlaylistIds playlist executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public Optional<PlaylistItem> findItemById(String itemId) {
        long inicio = System.currentTimeMillis();
        Optional<PlaylistItem> resultado = decorado.findItemById(itemId);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: findItemById playlist executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public boolean deleteItem(String itemId) {
        long inicio = System.currentTimeMillis();
        boolean resultado = decorado.deleteItem(itemId);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): deleteItem playlist executado em {}ms. itemId={} resultado={}", (fim - inicio), itemId, resultado);
        return resultado;
    }

    @Override
    public boolean alternarOcultaItem(String itemId) {
        long inicio = System.currentTimeMillis();
        boolean resultado = decorado.alternarOcultaItem(itemId);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): alternarOcultaItem playlist executado em {}ms. itemId={} resultado={}", (fim - inicio), itemId, resultado);
        return resultado;
    }
}

