package br.ufpb.dcx.projetos.album.repositories;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.infra.database.DatabaseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class AlbumDbRepository implements AlbumRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlbumDbRepository.class);

    private static final String COLUNAS = "id, titulo, artista, ano_lancamento, nota, usuario_id";

    private final ConnectionFactory connectionFactory;

    public AlbumDbRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void criar(Album album) {
        String sql = """
                INSERT INTO album (id, titulo, artista, ano_lancamento, nota, usuario_id)
                VALUES (?, ?, ?, ?, ?, ?)
                """;

        executarAtualizacao(sql, statement -> {
            statement.setString(1, album.getId());
            statement.setString(2, album.getTitulo());
            statement.setString(3, album.getArtista());
            statement.setInt(4, album.getAnoLancamento());
            if (album.getNota() != null) {
                statement.setDouble(5, album.getNota());
            } else {
                statement.setNull(5, java.sql.Types.DOUBLE);
            }
            statement.setString(6, album.getUsuarioId());
        });
        LOGGER.debug("ÃƒÂ lbum persistido. id={}", album.getId());
    }

    @Override
    public boolean atualizar(Album album) {
        String sql = """
                UPDATE album
                SET titulo = ?, artista = ?, ano_lancamento = ?, nota = ?
                WHERE id = ? AND usuario_id = ?
                """;

        int alterados = executarAtualizacao(sql, statement -> {
            statement.setString(1, album.getTitulo());
            statement.setString(2, album.getArtista());
            statement.setInt(3, album.getAnoLancamento());
            if (album.getNota() != null) {
                statement.setDouble(4, album.getNota());
            } else {
                statement.setNull(4, java.sql.Types.DOUBLE);
            }
            statement.setString(5, album.getId());
            statement.setString(6, album.getUsuarioId());
        });
        return alterados == 1;
    }

    @Override
    public Optional<Album> buscarPorId(String id, String usuarioId) {
        String sql = "SELECT " + COLUNAS + " FROM album WHERE id = ? AND usuario_id = ?";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, usuarioId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar ÃƒÂ¡lbum por ID", e);
        }
    }

    @Override
    public List<Album> listarTodos(String usuarioId) {
        String sql = "SELECT " + COLUNAS + " FROM album WHERE usuario_id = ? ORDER BY titulo, id";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuarioId);
            try (ResultSet result = statement.executeQuery()) {
                return mapearLista(result);
            }
        } catch (SQLException e) {
            throw erroPersistencia("listar ÃƒÂ¡lbuns", e);
        }
    }

    public List<Album> listarTodosGlobal() {
        String sql = "SELECT " + COLUNAS + " FROM album ORDER BY titulo, id";

        try (Connection connection = connectionFactory.abrir();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return mapearLista(result);
        } catch (SQLException e) {
            throw erroPersistencia("listar todos os álbuns globais", e);
        }
    }

    @Override
    public boolean remover(String id, String usuarioId) {
        String sql = "DELETE FROM album WHERE id = ? AND usuario_id = ?";
        return executarAtualizacao(sql, statement -> {
            statement.setString(1, id);
            statement.setString(2, usuarioId);
        }) == 1;
    }

    @Override
    public List<Album> buscar(String termo, String usuarioId) {
        if (termo == null || termo.trim().isEmpty()) {
            return listarTodos(usuarioId);
        }
        String sql = "SELECT id, titulo, artista, ano_lancamento, nota, usuario_id FROM album WHERE (titulo ILIKE ? OR artista ILIKE ?) AND usuario_id = ?;";
        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String wildcard = "%" + termo.trim() + "%";
            statement.setString(1, wildcard);
            statement.setString(2, wildcard);
            statement.setString(3, usuarioId);
            try (ResultSet result = statement.executeQuery()) {
                return mapearLista(result);
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar ÃƒÂ¡lbuns com filtro", e);
        }
    }

    private int executarAtualizacao(String sql, StatementBinder binder) {
        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw erroPersistencia("alterar dados de ÃƒÂ¡lbum", e);
        }
    }

    private List<Album> mapearLista(ResultSet result) throws SQLException {
        List<Album> albums = new ArrayList<>();
        while (result.next()) {
            albums.add(mapear(result));
        }
        return albums;
    }

    private Album mapear(ResultSet result) throws SQLException {
        Double nota = result.getObject("nota") != null ? result.getDouble("nota") : null;
        return new Album(
                result.getString("id"),
                result.getString("titulo"),
                result.getString("artista"),
                result.getInt("ano_lancamento"),
                nota,
                result.getString("usuario_id")
        );
    }

    private DatabaseException erroPersistencia(String operacao, SQLException causa) {
        return new DatabaseException("Falha ao " + operacao + ".", causa);
    }

    @FunctionalInterface
    private interface StatementBinder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
