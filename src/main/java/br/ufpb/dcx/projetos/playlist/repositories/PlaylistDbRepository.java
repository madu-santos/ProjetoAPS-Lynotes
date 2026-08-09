package br.ufpb.dcx.projetos.playlist.repositories;

import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.infra.database.DatabaseException;
import br.ufpb.dcx.projetos.playlist.models.Playlist;
import br.ufpb.dcx.projetos.playlist.models.PlaylistItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class PlaylistDbRepository implements PlaylistRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlaylistDbRepository.class);

    private final ConnectionFactory connectionFactory;

    public PlaylistDbRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void create(Playlist playlist) {
        String sql = """
                INSERT INTO playlist (id, nome, usuario_id, oculta, criado_em)
                VALUES (?, ?, ?, ?, ?)
                """;

        executarAtualizacao(sql, statement -> {
            statement.setString(1, playlist.getId());
            statement.setString(2, playlist.getNome());
            statement.setString(3, playlist.getUsuarioId());
            statement.setBoolean(4, playlist.isOculta());
            statement.setTimestamp(5, java.sql.Timestamp.from(playlist.getCriadoEm()));
        });

        LOGGER.debug("Playlist persistida. id={} nome={}", playlist.getId(), playlist.getNome());
    }

    @Override
    public Optional<Playlist> findById(String id) {
        String sql = "SELECT id, nome, usuario_id, oculta, criado_em FROM playlist WHERE id = ?";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapPlaylist(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar playlist por ID", e);
        }
    }

    @Override
    public List<Playlist> findByUsuarioId(String usuarioId) {
        String sql = "SELECT id, nome, usuario_id, oculta, criado_em FROM playlist WHERE usuario_id = ? ORDER BY criado_em DESC";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuarioId);
            try (ResultSet result = statement.executeQuery()) {
                List<Playlist> playlists = new ArrayList<>();
                while (result.next()) {
                    playlists.add(mapPlaylist(result));
                }
                return playlists;
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar playlists do usuário", e);
        }
    }

    @Override
    public List<Playlist> buscar(String termo, String usuarioId) {
        if (termo == null || termo.trim().isEmpty()) {
            return findByUsuarioId(usuarioId);
        }
        String sql = "SELECT id, nome, usuario_id, oculta, criado_em FROM playlist WHERE LOWER(nome) LIKE ? AND usuario_id = ? ORDER BY criado_em DESC";
        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String wildcard = "%" + termo.trim().toLowerCase() + "%";
            statement.setString(1, wildcard);
            statement.setString(2, usuarioId);
            try (ResultSet result = statement.executeQuery()) {
                List<Playlist> playlists = new ArrayList<>();
                while (result.next()) {
                    playlists.add(mapPlaylist(result));
                }
                return playlists;
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar playlists com filtro", e);
        }
    }

    @Override
    public void update(Playlist playlist) {
        String sql = "UPDATE playlist SET nome = ?, oculta = ? WHERE id = ?";
        executarAtualizacao(sql, statement -> {
            statement.setString(1, playlist.getNome());
            statement.setBoolean(2, playlist.isOculta());
            statement.setString(3, playlist.getId());
        });
        LOGGER.debug("Playlist atualizada. id={} nome={}", playlist.getId(), playlist.getNome());
    }

    @Override
    public boolean delete(String playlistId) {
        String sql = "DELETE FROM playlist WHERE id = ?";
        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playlistId);
            int affectedRows = statement.executeUpdate();
            return affectedRows > 0;
        } catch (SQLException e) {
            throw erroPersistencia("excluir playlist", e);
        }
    }

    @Override
    public int countItemsByPlaylistId(String playlistId) {
        String sql = "SELECT COUNT(*) FROM playlist_item WHERE playlist_id = ?";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playlistId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw erroPersistencia("contar itens da playlist", e);
        }
    }

    @Override
    public void createItem(PlaylistItem item) {
        String sql = """
                INSERT INTO playlist_item (id, playlist_id, url, video_id, titulo, ordem, oculta, criado_em)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executarAtualizacao(sql, statement -> {
            statement.setString(1, item.getId());
            statement.setString(2, item.getPlaylistId());
            statement.setString(3, item.getUrl());
            statement.setString(4, item.getVideoId());
            statement.setString(5, item.getTitulo());
            statement.setInt(6, item.getOrdem());
            statement.setBoolean(7, item.isOculta());
            statement.setTimestamp(8, java.sql.Timestamp.from(item.getCriadoEm()));
        });

        LOGGER.debug("Item adicionado à playlist. itemId={} playlistId={} url={}", item.getId(), item.getPlaylistId(), item.getUrl());
    }

    @Override
    public List<PlaylistItem> findItemsByPlaylistId(String playlistId) {
        String sql = "SELECT id, playlist_id, url, video_id, titulo, ordem, oculta, criado_em FROM playlist_item WHERE playlist_id = ? ORDER BY ordem, criado_em";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, playlistId);
            try (ResultSet result = statement.executeQuery()) {
                List<PlaylistItem> items = new ArrayList<>();
                while (result.next()) {
                    items.add(mapItem(result));
                }
                return items;
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar itens da playlist", e);
        }
    }

    @Override
    public List<PlaylistItem> findItemsByPlaylistIds(List<String> playlistIds) {
        if (playlistIds == null || playlistIds.isEmpty()) {
            return new ArrayList<>();
        }
        StringBuilder sql = new StringBuilder(
            "SELECT id, playlist_id, url, video_id, titulo, ordem, oculta, criado_em FROM playlist_item WHERE playlist_id IN ("
        );
        for (int i = 0; i < playlistIds.size(); i++) {
            sql.append("?");
            if (i < playlistIds.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(") ORDER BY playlist_id, ordem, criado_em");

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < playlistIds.size(); i++) {
                statement.setString(i + 1, playlistIds.get(i));
            }
            try (ResultSet result = statement.executeQuery()) {
                List<PlaylistItem> items = new ArrayList<>();
                while (result.next()) {
                    items.add(mapItem(result));
                }
                return items;
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar itens de playlists em lote", e);
        }
    }

    @Override
    public Optional<PlaylistItem> findItemById(String itemId) {
        String sql = "SELECT id, playlist_id, url, video_id, titulo, ordem, oculta, criado_em FROM playlist_item WHERE id = ?";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, itemId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapItem(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar item da playlist por ID", e);
        }
    }

    @Override
    public boolean deleteItem(String itemId) {
        String sql = "DELETE FROM playlist_item WHERE id = ?";
        return executarAtualizacao(sql, statement -> statement.setString(1, itemId)) == 1;
    }

    @Override
    public boolean alternarOcultaItem(String itemId) {
        String sql = "UPDATE playlist_item SET oculta = NOT oculta WHERE id = ?";
        return executarAtualizacao(sql, statement -> statement.setString(1, itemId)) == 1;
    }

    private Playlist mapPlaylist(ResultSet result) throws SQLException {
        java.sql.Timestamp criadoEmTimestamp = result.getTimestamp("criado_em");
        return new Playlist(
                result.getString("id"),
                result.getString("nome"),
                result.getString("usuario_id"),
                result.getBoolean("oculta"),
                criadoEmTimestamp != null ? criadoEmTimestamp.toInstant() : null
        );
    }

    private PlaylistItem mapItem(ResultSet result) throws SQLException {
        java.sql.Timestamp criadoEmTimestamp = result.getTimestamp("criado_em");
        boolean oculta = false;
        try {
            oculta = result.getBoolean("oculta");
        } catch (SQLException ignored) {}
        return new PlaylistItem(
                result.getString("id"),
                result.getString("playlist_id"),
                result.getString("url"),
                result.getString("video_id"),
                result.getString("titulo"),
                result.getInt("ordem"),
                oculta,
                criadoEmTimestamp != null ? criadoEmTimestamp.toInstant() : null
        );
    }

    private int executarAtualizacao(String sql, StatementBinder binder) {
        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw erroPersistencia("alterar dados de playlist", e);
        }
    }

    private DatabaseException erroPersistencia(String operacao, SQLException causa) {
        return new DatabaseException("Falha ao " + operacao + ".", causa);
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
