package br.ufpb.dcx.projetos.artista.repositories;

import br.ufpb.dcx.projetos.artista.models.Artista;
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

public final class ArtistaDbRepository implements ArtistaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtistaDbRepository.class);

    private static final String COLUNAS = "id, nome, genero_musical, biografia";

    private final ConnectionFactory connectionFactory;

    public ArtistaDbRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void criar(Artista artista) {
        String sql = """
                INSERT INTO artista (id, nome, genero_musical, biografia)
                VALUES (?, ?, ?, ?)
                """;

        executarAtualizacao(sql, statement -> preencherArtista(statement, artista));
        LOGGER.debug("Artista persistido. id={}", artista.getId());
    }

    @Override
    public boolean atualizar(Artista artista) {
        String sql = """
                UPDATE artista
                SET nome = ?, genero_musical = ?, biografia = ?
                WHERE id = ?
                """;

        int alterados = executarAtualizacao(sql, statement -> {
            statement.setString(1, artista.getNome());
            statement.setString(2, artista.getGeneroMusical());
            statement.setString(3, artista.getBiografia());
            statement.setString(4, artista.getId());
        });
        return alterados == 1;
    }

    @Override
    public Optional<Artista> buscarPorId(String id) {
        String sql = "SELECT " + COLUNAS + " FROM artista WHERE id = ?";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar artista por ID", e);
        }
    }

    @Override
    public List<Artista> listarTodos() {
        String sql = "SELECT " + COLUNAS + " FROM artista ORDER BY nome, id";

        try (Connection connection = connectionFactory.abrir();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return mapearLista(result);
        } catch (SQLException e) {
            throw erroPersistencia("listar artistas", e);
        }
    }

    @Override
    public List<Artista> buscar(String termo) {
        String sql = """
                SELECT %s
                FROM artista
                WHERE nome ILIKE ?
                   OR genero_musical ILIKE ?
                   OR biografia ILIKE ?
                ORDER BY nome, id
                """.formatted(COLUNAS);
        String padrao = "%" + termo + "%";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            preencherBusca(statement, padrao);
            try (ResultSet result = statement.executeQuery()) {
                return mapearLista(result);
            }
        } catch (SQLException e) {
            throw erroPersistencia("filtrar artistas", e);
        }
    }

    @Override
    public boolean remover(String id) {
        String sql = "DELETE FROM artista WHERE id = ?";
        return executarAtualizacao(sql, statement -> statement.setString(1, id)) == 1;
    }

    private int executarAtualizacao(String sql, StatementBinder binder) {
        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw erroPersistencia("alterar dados de artista", e);
        }
    }

    private void preencherArtista(PreparedStatement statement, Artista artista) throws SQLException {
        statement.setString(1, artista.getId());
        statement.setString(2, artista.getNome());
        statement.setString(3, artista.getGeneroMusical());
        statement.setString(4, artista.getBiografia());
    }

    private void preencherBusca(PreparedStatement statement, String padrao) throws SQLException {
        statement.setString(1, padrao);
        statement.setString(2, padrao);
        statement.setString(3, padrao);
    }

    private List<Artista> mapearLista(ResultSet result) throws SQLException {
        List<Artista> artistas = new ArrayList<>();
        while (result.next()) {
            artistas.add(mapear(result));
        }
        return artistas;
    }

    private Artista mapear(ResultSet result) throws SQLException {
        return new Artista(
                result.getString("id"),
                result.getString("nome"),
                result.getString("genero_musical"),
                result.getString("biografia")
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
