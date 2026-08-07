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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ArtistaDbRepository implements ArtistaRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtistaDbRepository.class);

    private static final String COLUNAS = "id, nome, genero_musical, biografia, cpf, data_nascimento, cep, logradouro, numero, bairro, cidade, uf, status_verificacao, nota, usuario_id, youtube_id, capa_url";

    private final ConnectionFactory connectionFactory;

    public ArtistaDbRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void criar(Artista artista) {
        String sql = """
                INSERT INTO artista (id, nome, genero_musical, biografia, cpf, data_nascimento, cep, logradouro, numero, bairro, cidade, uf, status_verificacao, nota, usuario_id, youtube_id, capa_url)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executarAtualizacao(sql, statement -> preencherArtista(statement, artista, true));
        LOGGER.debug("Artista persistido. id={}", artista.getId());
    }

    @Override
    public boolean atualizar(Artista artista) {
        String sql = """
                UPDATE artista
                SET nome = ?, genero_musical = ?, biografia = ?, cpf = ?, data_nascimento = ?, cep = ?, logradouro = ?, numero = ?, bairro = ?, cidade = ?, uf = ?, status_verificacao = ?, nota = ?, youtube_id = ?, capa_url = ?
                WHERE id = ? AND usuario_id = ?
                """;

        int alterados = executarAtualizacao(sql, statement -> {
            preencherArtista(statement, artista, false);
            statement.setString(16, artista.getId());
            statement.setString(17, artista.getUsuarioId());
        });
        return alterados == 1;
    }

    @Override
    public Optional<Artista> buscarPorId(String id, String usuarioId) {
        String sql = "SELECT " + COLUNAS + " FROM artista WHERE id = ? AND usuario_id = ?";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, id);
            statement.setString(2, usuarioId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar artista por ID", e);
        }
    }

    @Override
    public Optional<Artista> buscarPorYouTubeId(String youtubeId) {
        if (youtubeId == null || youtubeId.isBlank()) return Optional.empty();
        String sql = "SELECT " + COLUNAS + " FROM artista WHERE youtube_id = ? LIMIT 1";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, youtubeId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar artista por ID do YouTube", e);
        }
    }

    @Override
    public List<Artista> listarTodos(String usuarioId) {
        String sql = "SELECT " + COLUNAS + " FROM artista WHERE usuario_id = ? ORDER BY nome, id";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuarioId);
            try (ResultSet result = statement.executeQuery()) {
                return mapearLista(result);
            }
        } catch (SQLException e) {
            throw erroPersistencia("listar artistas", e);
        }
    }

    public List<Artista> listarTodosGlobal() {
        String sql = "SELECT " + COLUNAS + " FROM artista ORDER BY nome, id";

        try (Connection connection = connectionFactory.abrir();
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(sql)) {
            return mapearLista(result);
        } catch (SQLException e) {
            throw erroPersistencia("listar todos artistas", e);
        }
    }

    @Override
    public List<Artista> buscar(String termo, String usuarioId) {
        String sql = """
                SELECT %s
                FROM artista
                WHERE (LOWER(nome) LIKE ?
                   OR LOWER(genero_musical) LIKE ?
                   OR LOWER(biografia) LIKE ?)
                   AND usuario_id = ?
                ORDER BY nome, id
                """.formatted(COLUNAS);
        String padrao = "%" + termo.trim().toLowerCase() + "%";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            preencherBusca(statement, padrao);
            statement.setString(4, usuarioId);
            try (ResultSet result = statement.executeQuery()) {
                return mapearLista(result);
            }
        } catch (SQLException e) {
            throw erroPersistencia("filtrar artistas", e);
        }
    }

    @Override
    public List<Artista> listarPendentes() {
        String sql = "SELECT " + COLUNAS + " FROM artista WHERE status_verificacao = 'PENDENTE' ORDER BY nome, id";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            return mapearLista(result);
        } catch (SQLException e) {
            throw erroPersistencia("listar artistas pendentes", e);
        }
    }

    @Override
    public boolean atualizarStatusVerificacao(String id, br.ufpb.dcx.projetos.artista.models.StatusVerificacao status) {
        String sql = "UPDATE artista SET status_verificacao = ? WHERE id = ?";
        return executarAtualizacao(sql, statement -> {
            statement.setString(1, status.name());
            statement.setString(2, id);
        }) == 1;
    }

    @Override
    public boolean remover(String id, String usuarioId) {
        String sql = "DELETE FROM artista WHERE id = ? AND usuario_id = ?";
        return executarAtualizacao(sql, statement -> {
            statement.setString(1, id);
            statement.setString(2, usuarioId);
        }) == 1;
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

    private void preencherArtista(PreparedStatement statement, Artista artista, boolean isInsert) throws SQLException {
        if (isInsert) {
            statement.setString(1, artista.getId());
            statement.setString(2, artista.getNome());
            statement.setString(3, artista.getGeneroMusical());
            statement.setString(4, artista.getBiografia());
            statement.setString(5, artista.getCpf());
            statement.setObject(6, artista.getDataNascimento());
            statement.setString(7, artista.getCep());
            statement.setString(8, artista.getLogradouro());
            statement.setString(9, artista.getNumero());
            statement.setString(10, artista.getBairro());
            statement.setString(11, artista.getCidade());
            statement.setString(12, artista.getUf());
            statement.setString(13, artista.getStatusVerificacao().name());
            if (artista.getNota() != null) {
                statement.setDouble(14, artista.getNota());
            } else {
                statement.setNull(14, java.sql.Types.DOUBLE);
            }
            statement.setString(15, artista.getUsuarioId());
            statement.setString(16, artista.getYoutubeId());
            statement.setString(17, artista.getCapaUrl());
        } else {
            statement.setString(1, artista.getNome());
            statement.setString(2, artista.getGeneroMusical());
            statement.setString(3, artista.getBiografia());
            statement.setString(4, artista.getCpf());
            statement.setObject(5, artista.getDataNascimento());
            statement.setString(6, artista.getCep());
            statement.setString(7, artista.getLogradouro());
            statement.setString(8, artista.getNumero());
            statement.setString(9, artista.getBairro());
            statement.setString(10, artista.getCidade());
            statement.setString(11, artista.getUf());
            statement.setString(12, artista.getStatusVerificacao().name());
            if (artista.getNota() != null) {
                statement.setDouble(13, artista.getNota());
            } else {
                statement.setNull(13, java.sql.Types.DOUBLE);
            }
            statement.setString(14, artista.getYoutubeId());
            statement.setString(15, artista.getCapaUrl());
        }
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
        java.sql.Date sqlDate = result.getDate("data_nascimento");
        LocalDate data = sqlDate != null ? sqlDate.toLocalDate() : null;
        Double nota = result.getObject("nota") != null ? result.getDouble("nota") : null;

        return new Artista(
                result.getString("id"),
                result.getString("nome"),
                result.getString("genero_musical"),
                result.getString("biografia"),
                result.getString("cpf"),
                data,
                result.getString("cep"),
                result.getString("logradouro"),
                result.getString("numero"),
                result.getString("bairro"),
                result.getString("cidade"),
                result.getString("uf"),
                br.ufpb.dcx.projetos.artista.models.StatusVerificacao.valueOf(result.getString("status_verificacao")),
                nota,
                result.getString("usuario_id"),
                result.getString("youtube_id"),
                result.getString("capa_url")
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
