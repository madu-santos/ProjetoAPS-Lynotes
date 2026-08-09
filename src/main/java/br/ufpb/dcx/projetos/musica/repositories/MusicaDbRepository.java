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

    private static final String COLUNAS = "id, titulo, artista, genero, duracao_segundos, resenha, nota, spotify_url, youtube_url, album_id, usuario_id, youtube_id, capa_url, oculta_da_biblioteca";

    private final ConnectionFactory connectionFactory;

    public MusicaDbRepository(ConnectionFactory connectionFactory) {
        this.connectionFactory = connectionFactory;
    }

    @Override
    public void criar(Musica musica) {
        String sql = """
                INSERT INTO musica (id, titulo, artista, genero, duracao_segundos, resenha, nota, spotify_url, youtube_url, album_id, usuario_id, youtube_id, capa_url, oculta_da_biblioteca)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        executarAtualizacao(sql, statement -> {
            statement.setString(1, musica.getId());
            statement.setString(2, musica.getTitulo());
            statement.setString(3, musica.getArtista());
            statement.setString(4, musica.getGenero());
            
            if (musica.getDuracaoSegundos() != null) {
                statement.setInt(5, musica.getDuracaoSegundos());
            } else {
                statement.setNull(5, Types.INTEGER);
            }
            
            statement.setString(6, musica.getResenha());
            
            if (musica.getNota() != null) {
                statement.setDouble(7, musica.getNota());
            } else {
                statement.setNull(7, Types.DOUBLE);
            }
            
            statement.setString(8, musica.getSpotifyUrl());
            statement.setString(9, musica.getYoutubeUrl());
            
            if (musica.getAlbumId() != null && !musica.getAlbumId().isBlank()) {
                statement.setString(10, musica.getAlbumId());
            } else {
                statement.setNull(10, Types.VARCHAR);
            }
            
            if (musica.getUsuarioId() != null && !musica.getUsuarioId().isBlank()) {
                statement.setString(11, musica.getUsuarioId());
            } else {
                statement.setNull(11, Types.VARCHAR);
            }

            statement.setString(12, musica.getYoutubeId());
            statement.setString(13, musica.getCapaUrl());
            statement.setBoolean(14, musica.isOcultaDaBiblioteca());
        });
        LOGGER.debug("Música persistida no catálogo. id={}", musica.getId());
    }

    @Override
    public boolean atualizar(Musica musica) {
        String sql = """
                UPDATE musica
                SET titulo = ?, artista = ?, genero = ?, duracao_segundos = ?, resenha = ?, nota = ?, spotify_url = ?, youtube_url = ?, album_id = ?, usuario_id = ?, youtube_id = ?, capa_url = ?, oculta_da_biblioteca = ?
                WHERE id = ?
                """;

        int alterados = executarAtualizacao(sql, statement -> {
            statement.setString(1, musica.getTitulo());
            statement.setString(2, musica.getArtista());
            statement.setString(3, musica.getGenero());
            
            if (musica.getDuracaoSegundos() != null) {
                statement.setInt(4, musica.getDuracaoSegundos());
            } else {
                statement.setNull(4, Types.INTEGER);
            }
            
            statement.setString(5, musica.getResenha());
            
            if (musica.getNota() != null) {
                statement.setDouble(6, musica.getNota());
            } else {
                statement.setNull(6, Types.DOUBLE);
            }
            
            statement.setString(7, musica.getSpotifyUrl());
            statement.setString(8, musica.getYoutubeUrl());
            
            if (musica.getAlbumId() != null && !musica.getAlbumId().isBlank()) {
                statement.setString(9, musica.getAlbumId());
            } else {
                statement.setNull(9, Types.VARCHAR);
            }
            
            if (musica.getUsuarioId() != null && !musica.getUsuarioId().isBlank()) {
                statement.setString(10, musica.getUsuarioId());
            } else {
                statement.setNull(10, Types.VARCHAR);
            }

            statement.setString(11, musica.getYoutubeId());
            statement.setString(12, musica.getCapaUrl());
            statement.setBoolean(13, musica.isOcultaDaBiblioteca());
            statement.setString(14, musica.getId());
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
    public Optional<Musica> buscarPorYouTubeId(String youtubeId) {
        if (youtubeId == null || youtubeId.isBlank()) return Optional.empty();
        String sql = "SELECT " + COLUNAS + " FROM musica WHERE youtube_id = ? LIMIT 1";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, youtubeId);
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? Optional.of(mapear(result)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar música por ID do YouTube", e);
        }
    }

    @Override
    public List<Musica> listarTodas() {
        String sql = "SELECT " + COLUNAS + " FROM musica ORDER BY criado_em DESC, id DESC";

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
        String sql = "SELECT " + COLUNAS + " FROM musica WHERE album_id = ? ORDER BY criado_em ASC, id ASC";

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
    public List<Musica> buscarPorUsuarioId(String usuarioId) {
        String sql = "SELECT " + COLUNAS + " FROM musica WHERE usuario_id = ? AND (album_id IS NULL OR album_id = '') AND oculta_da_biblioteca = FALSE ORDER BY criado_em DESC, id DESC";

        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, usuarioId);
            try (ResultSet result = statement.executeQuery()) {
                return mapearLista(result);
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar músicas por ID do usuário", e);
        }
    }

    @Override
    public List<Musica> buscar(String termo, String usuarioId) {
        if (termo == null || termo.trim().isEmpty()) {
            return buscarPorUsuarioId(usuarioId);
        }
        String sql = "SELECT " + COLUNAS + " FROM musica WHERE (LOWER(titulo) LIKE ? OR LOWER(artista) LIKE ? OR LOWER(genero) LIKE ? OR LOWER(resenha) LIKE ?) AND usuario_id = ? AND (album_id IS NULL OR album_id = '') AND oculta_da_biblioteca = FALSE ORDER BY criado_em DESC, id DESC;";
        try (Connection connection = connectionFactory.abrir();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            String wildcard = "%" + termo.trim().toLowerCase() + "%";
            statement.setString(1, wildcard);
            statement.setString(2, wildcard);
            statement.setString(3, wildcard);
            statement.setString(4, wildcard);
            statement.setString(5, usuarioId);
            try (ResultSet result = statement.executeQuery()) {
                return mapearLista(result);
            }
        } catch (SQLException e) {
            throw erroPersistencia("buscar músicas com filtro", e);
        }
    }

    @Override
    public boolean alternarOculta(String id, String usuarioId) {
        String sql = "UPDATE musica SET oculta_da_biblioteca = NOT oculta_da_biblioteca WHERE id = ? AND usuario_id = ?";
        return executarAtualizacao(sql, statement -> {
            statement.setString(1, id);
            statement.setString(2, usuarioId);
        }) == 1;
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
            throw erroPersistencia("alterar dados de mÃºsica", e);
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
                result.getString("artista"),
                result.getString("genero"),
                result.getObject("duracao_segundos", Integer.class),
                result.getString("resenha"),
                result.getObject("nota") != null ? result.getDouble("nota") : null,
                result.getString("spotify_url"),
                result.getString("youtube_url"),
                result.getString("album_id"),
                result.getString("usuario_id"),
                result.getString("youtube_id"),
                result.getString("capa_url"),
                result.getBoolean("oculta_da_biblioteca")
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
