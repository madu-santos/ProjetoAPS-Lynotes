package br.ufpb.dcx.projetos.musica.repositories;

import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.infra.database.DatabaseException;
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

public final class MusicaDbRepository implements MusicaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(MusicaDbRepository.class);

    private static final String COLUNAS = "id, titulo, genero, duracao_segundos, album_id";

    private final ConnectionFactory connectionFactory;

    public MusicaDbRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void criar(Musica musica) {
        String sql = """
                INSERT INTO musica (id, titulo, genero, duracao_segundos, album_id)
                VALUES (?, ?, ?, ?, ?)
                """;

        executarAtualizacao(sql, statement -> {
            statement.setString(1, musica.getId());
            statement.setString(2, musica.getTitulo());
            statement.setString(3, musica.getGenero());
            statement.setInt(4, musica.getDuracaoSegundos());
            if (musica.getAlbumId() != null && !musica.getAlbumId().isBlank()) {
                statement.setString(5, musica.getAlbumId());
            } else {
                statement.setNull(5, Types.VARCHAR);
            }
        });
        LOGGER.debug("Música persistida. id={}", musica.getId());
    }

    @Override
    public boolean atualizar(Musica musica) {
        String sql = """
                UPDATE musica
                SET titulo = ?, genero = ?, duracao_segundos = ?, album_id = ?
                WHERE id = ?
                """;

        int alterados = executarAtualizacao(sql, statement -> {
            statement.setString(1, musica.getTitulo());
            statement.setString(2, musica.getGenero());
            statement.setInt(3, musica.getDuracaoSegundos());
            if (musica.getAlbumId() != null && !musica.getAlbumId().isBlank()) {
                statement.setString(4, musica.getAlbumId());
            } else {
                statement.setNull(4, Types.VARCHAR);
            }
            statement.setString(5, musica.getId());
        });
        return alterados == 1;
    }

    @Override
    public Optional<Musica> buscarPorId(String id) {
        String sql = "SELECT " + COLUNAS + " FROM musica WHERE id = ?";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar música por ID", e);
        }
    }

    @Override
    public List<Musica> listarTodas() {
        String sql = "SELECT " + COLUNAS + " FROM musica ORDER BY titulo, id";

        try (Connection connection = connectionFactory.abrir();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return mapearLista(result);
        } catch (SQLException e) {
            throw erroPersistencia("listar músicas", e);
        }
    }

    @Override
    public List<Musica> buscarPorAlbumId(String albumId) {
        String sql = "SELECT " + COLUNAS + " FROM musica WHERE album_id = ? ORDER BY titulo, id";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, albumId);
            try (ResultSet result = statement.executeQuery()) {
                return mapearLista(result);
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar músicas por ID do álbum", e);
        }
    }

    @Override
    public boolean remover(String id) {
        String sql = "DELETE FROM musica WHERE id = ?";
        return executarAtualizacao(sql, statement -> statement.setString(1, id)) == 1;
    }

    private int executarAtualizacao(String sql, StatementBinder binder) {
        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw erroPersistencia("alterar dados de música", e);
        }
    }

    private List<Musica> mapearLista(ResultSet result) throws SQLException {
        List<Musica> musicas = new ArrayList<>();
        while (result.next()) {
            musicas.add(mapear(result));
        }
        return musicas;
    }

    private Musica mapear(ResultSet result) throws SQLException {
        return new Musica(
                result.getString("id"),
                result.getString("titulo"),
                result.getString("genero"),
                result.getInt("duracao_segundos"),
                result.getString("album_id")
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
