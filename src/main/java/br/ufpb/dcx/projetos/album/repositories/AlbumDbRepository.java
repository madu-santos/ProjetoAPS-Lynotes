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

    private static final String COLUNAS = "id, titulo, artista, ano_lancamento";

    private final ConnectionFactory connectionFactory;

    public AlbumDbRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void criar(Album album) {
        String sql = """
                INSERT INTO album (id, titulo, artista, ano_lancamento)
                VALUES (?, ?, ?, ?)
                """;

        executarAtualizacao(sql, statement -> {
            statement.setString(1, album.getId());
            statement.setString(2, album.getTitulo());
            statement.setString(3, album.getArtista());
            statement.setInt(4, album.getAnoLancamento());
        });
        LOGGER.debug("Álbum persistido. id={}", album.getId());
    }

    @Override
    public boolean atualizar(Album album) {
        String sql = """
                UPDATE album
                SET titulo = ?, artista = ?, ano_lancamento = ?
                WHERE id = ?
                """;

        int alterados = executarAtualizacao(sql, statement -> {
            statement.setString(1, album.getTitulo());
            statement.setString(2, album.getArtista());
            statement.setInt(3, album.getAnoLancamento());
            statement.setString(4, album.getId());
        });
        return alterados == 1;
    }

    @Override
    public Optional<Album> buscarPorId(String id) {
        String sql = "SELECT " + COLUNAS + " FROM album WHERE id = ?";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar álbum por ID", e);
        }
    }

    @Override
    public List<Album> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM album ORDER BY titulo, id";

        try (Connection connection = connectionFactory.abrir();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return mapearLista(result);
        } catch (SQLException e) {
            throw erroPersistencia("listar álbuns", e);
        }
    }

    @Override
    public boolean remover(String id) {
        String sql = "DELETE FROM album WHERE id = ?";
        return executarAtualizacao(sql, statement -> statement.setString(1, id)) == 1;
    }

    private int executarAtualizacao(String sql, StatementBinder binder) {
        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw erroPersistencia("alterar dados de álbum", e);
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
        return new Album(
                result.getString("id"),
                result.getString("titulo"),
                result.getString("artista"),
                result.getInt("ano_lancamento")
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
