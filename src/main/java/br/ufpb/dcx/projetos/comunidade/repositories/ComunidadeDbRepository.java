package br.ufpb.dcx.projetos.comunidade.repositories;

import br.ufpb.dcx.projetos.comunidade.models.Comentario;
import br.ufpb.dcx.projetos.comunidade.models.Post;
import br.ufpb.dcx.projetos.comunidade.views.ComentarioViewDTO;
import br.ufpb.dcx.projetos.comunidade.views.PostViewDTO;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import br.ufpb.dcx.projetos.comunidade.views.NotificacaoViewDTO;

public class ComunidadeDbRepository {

    private final ConnectionFactory factory;

    public ComunidadeDbRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    public void salvarPost(Post post) {
        String sql = "INSERT INTO post (id, usuario_id, conteudo, data_criacao, tipo, musica_id, album_id, artista_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?) " +
                     "ON CONFLICT (id) DO UPDATE SET conteudo = EXCLUDED.conteudo, tipo = EXCLUDED.tipo, " +
                     "musica_id = EXCLUDED.musica_id, album_id = EXCLUDED.album_id, artista_id = EXCLUDED.artista_id;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, post.getId());
            ps.setString(2, post.getUsuarioId());
            ps.setString(3, post.getConteudo());
            ps.setTimestamp(4, Timestamp.valueOf(post.getDataCriacao()));
            ps.setString(5, post.getTipo());
            ps.setString(6, post.getMusicaId());
            ps.setString(7, post.getAlbumId());
            ps.setString(8, post.getArtistaId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar post: " + e.getMessage(), e);
        }
    }

    public Optional<Post> buscarPostPorId(String id) {
        String sql = "SELECT * FROM post WHERE id = ?";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Post(
                            rs.getString("id"),
                            rs.getString("usuario_id"),
                            rs.getString("conteudo"),
                            rs.getTimestamp("data_criacao").toLocalDateTime(),
                            rs.getString("tipo"),
                            rs.getString("musica_id"),
                            rs.getString("album_id"),
                            rs.getString("artista_id")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar post: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void salvarComentario(Comentario comentario) {
        String sql = "INSERT INTO comentario (id, post_id, usuario_id, conteudo, data_criacao) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, comentario.getId());
            ps.setString(2, comentario.getPostId());
            ps.setString(3, comentario.getUsuarioId());
            ps.setString(4, comentario.getConteudo());
            ps.setTimestamp(5, Timestamp.valueOf(comentario.getDataCriacao()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar comentÃ¡rio: " + e.getMessage(), e);
        }
    }

    public boolean darEstrela(String postId, String usuarioId) {
        String checkSql = "SELECT 1 FROM post_estrela WHERE post_id = ? AND usuario_id = ?";
        String insertSql = "INSERT INTO post_estrela (post_id, usuario_id) VALUES (?, ?)";
        String deleteSql = "DELETE FROM post_estrela WHERE post_id = ? AND usuario_id = ?";

        try (Connection conn = factory.abrir()) {
            boolean jaEstrelado = false;
            try (PreparedStatement checkPs = conn.prepareStatement(checkSql)) {
                checkPs.setString(1, postId);
                checkPs.setString(2, usuarioId);
                try (ResultSet rs = checkPs.executeQuery()) {
                    jaEstrelado = rs.next();
                }
            }

            if (jaEstrelado) {
                try (PreparedStatement delPs = conn.prepareStatement(deleteSql)) {
                    delPs.setString(1, postId);
                    delPs.setString(2, usuarioId);
                    delPs.executeUpdate();
                }
                return false;
            } else {
                try (PreparedStatement insPs = conn.prepareStatement(insertSql)) {
                    insPs.setString(1, postId);
                    insPs.setString(2, usuarioId);
                    insPs.executeUpdate();
                }
                return true;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao processar estrela no post: " + e.getMessage(), e);
        }
    }

    private PostViewDTO mapPostView(ResultSet rs) throws SQLException {
        return new PostViewDTO(
                rs.getString("id"),
                rs.getString("usuario_id"),
                rs.getString("usuario_nome"),
                rs.getString("usuario_username"),
                rs.getString("usuario_foto_url"),
                rs.getString("conteudo"),
                rs.getTimestamp("data_criacao").toLocalDateTime(),
                rs.getInt("qtd_estrelas"),
                rs.getInt("qtd_comentarios"),
                rs.getBoolean("estrelado_por_mim"),
                rs.getString("tipo"),
                rs.getString("musica_id"),
                rs.getString("musica_titulo"),
                rs.getString("musica_artista"),
                rs.getObject("musica_nota") != null ? rs.getDouble("musica_nota") : null,
                rs.getString("musica_youtube_url"),
                rs.getString("musica_capa_url"),
                rs.getString("album_id"),
                rs.getString("album_titulo"),
                rs.getString("album_artista"),
                rs.getObject("album_nota") != null ? rs.getDouble("album_nota") : null,
                rs.getString("album_capa_url"),
                rs.getString("artista_id"),
                rs.getString("artista_nome"),
                rs.getString("artista_capa_url")
        );
    }

    public List<PostViewDTO> listarFeed(String usuarioLogadoId, String filtroUsuarioId) {
        List<PostViewDTO> feed = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT p.id, p.usuario_id, u.nome AS usuario_nome, u.username AS usuario_username, u.foto_url AS usuario_foto_url, " +
            "p.conteudo, p.data_criacao, p.tipo, p.musica_id, p.album_id, p.artista_id, " +
            "(SELECT COUNT(*) FROM post_estrela pe WHERE pe.post_id = p.id) AS qtd_estrelas, " +
            "(SELECT COUNT(*) FROM comentario c WHERE c.post_id = p.id) AS qtd_comentarios, " +
            "(SELECT COUNT(*) > 0 FROM post_estrela pe2 WHERE pe2.post_id = p.id AND pe2.usuario_id = ?) AS estrelado_por_mim, " +
            "m.titulo AS musica_titulo, m.artista AS musica_artista, m.nota AS musica_nota, m.youtube_url AS musica_youtube_url, m.capa_url AS musica_capa_url, " +
            "al.titulo AS album_titulo, al.artista AS album_artista, al.nota AS album_nota, al.capa_url AS album_capa_url, " +
            "ar.nome AS artista_nome, ar.capa_url AS artista_capa_url " +
            "FROM post p " +
            "JOIN usuario u ON p.usuario_id = u.id " +
            "LEFT JOIN musica m ON p.musica_id = m.id " +
            "LEFT JOIN album al ON p.album_id = al.id " +
            "LEFT JOIN artista ar ON p.artista_id = ar.id "
        );
        
        if (filtroUsuarioId == null || "seguindo".equals(filtroUsuarioId)) {
            sql.append("WHERE p.usuario_id IN (SELECT s1.seguido_id FROM seguidor s1 JOIN seguidor s2 ON s1.seguido_id = s2.seguidor_id WHERE s1.seguidor_id = ? AND s2.seguido_id = ?) ");
        } else {
            sql.append("WHERE p.usuario_id = ? ");
        }
        
        sql.append("ORDER BY p.data_criacao DESC");

        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            ps.setString(1, usuarioLogadoId != null ? usuarioLogadoId : "");
            
            if (filtroUsuarioId == null || "seguindo".equals(filtroUsuarioId)) {
                ps.setString(2, usuarioLogadoId != null ? usuarioLogadoId : "");
                ps.setString(3, usuarioLogadoId != null ? usuarioLogadoId : "");
            } else {
                ps.setString(2, filtroUsuarioId);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    feed.add(mapPostView(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar feed: " + e.getMessage(), e);
        }
        return feed;
    }

    public Optional<PostViewDTO> buscarPostView(String postId, String usuarioLogadoId) {
        String sql = "SELECT p.id, p.usuario_id, u.nome AS usuario_nome, u.username AS usuario_username, u.foto_url AS usuario_foto_url, " +
            "p.conteudo, p.data_criacao, p.tipo, p.musica_id, p.album_id, p.artista_id, " +
            "(SELECT COUNT(*) FROM post_estrela pe WHERE pe.post_id = p.id) AS qtd_estrelas, " +
            "(SELECT COUNT(*) FROM comentario c WHERE c.post_id = p.id) AS qtd_comentarios, " +
            "(SELECT COUNT(*) > 0 FROM post_estrela pe2 WHERE pe2.post_id = p.id AND pe2.usuario_id = ?) AS estrelado_por_mim, " +
            "m.titulo AS musica_titulo, m.artista AS musica_artista, m.nota AS musica_nota, m.youtube_url AS musica_youtube_url, m.capa_url AS musica_capa_url, " +
            "al.titulo AS album_titulo, al.artista AS album_artista, al.nota AS album_nota, al.capa_url AS album_capa_url, " +
            "ar.nome AS artista_nome, ar.capa_url AS artista_capa_url " +
            "FROM post p " +
            "JOIN usuario u ON p.usuario_id = u.id " +
            "LEFT JOIN musica m ON p.musica_id = m.id " +
            "LEFT JOIN album al ON p.album_id = al.id " +
            "LEFT JOIN artista ar ON p.artista_id = ar.id " +
            "WHERE p.id = ?";
            
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioLogadoId != null ? usuarioLogadoId : "");
            ps.setString(2, postId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapPostView(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar post view: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public List<ComentarioViewDTO> listarComentarios(String postId) {
        List<ComentarioViewDTO> comentarios = new ArrayList<>();
        String sql = "SELECT c.id, c.post_id, c.usuario_id, u.nome AS usuario_nome, c.conteudo, c.data_criacao " +
                     "FROM comentario c " +
                     "JOIN usuario u ON c.usuario_id = u.id " +
                     "WHERE c.post_id = ? " +
                     "ORDER BY c.data_criacao ASC";
                     
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comentarios.add(new ComentarioViewDTO(
                            rs.getString("id"),
                            rs.getString("post_id"),
                            rs.getString("usuario_id"),
                            rs.getString("usuario_nome"),
                            rs.getString("conteudo"),
                            rs.getTimestamp("data_criacao").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar comentarios: " + e.getMessage(), e);
        }
        return comentarios;
    }

    public Map<String, List<ComentarioViewDTO>> listarComentariosParaPosts(List<String> postIds) {
        Map<String, List<ComentarioViewDTO>> resultado = new HashMap<>();
        if (postIds == null || postIds.isEmpty()) {
            return resultado;
        }
        for (String id : postIds) {
            resultado.put(id, new ArrayList<>());
        }
        
        StringBuilder sql = new StringBuilder(
            "SELECT c.id, c.post_id, c.usuario_id, u.nome AS usuario_nome, c.conteudo, c.data_criacao " +
            "FROM comentario c " +
            "JOIN usuario u ON c.usuario_id = u.id " +
            "WHERE c.post_id IN ("
        );
        for (int i = 0; i < postIds.size(); i++) {
            sql.append("?");
            if (i < postIds.size() - 1) {
                sql.append(",");
            }
        }
        sql.append(") ORDER BY c.data_criacao ASC");

        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < postIds.size(); i++) {
                ps.setString(i + 1, postIds.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String postId = rs.getString("post_id");
                    resultado.get(postId).add(new ComentarioViewDTO(
                            rs.getString("id"),
                            postId,
                            rs.getString("usuario_id"),
                            rs.getString("usuario_nome"),
                            rs.getString("conteudo"),
                            rs.getTimestamp("data_criacao").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar comentarios em lote: " + e.getMessage(), e);
        }
        return resultado;
    }


    public Optional<Comentario> buscarComentarioPorId(String comentarioId) {
        String sql = "SELECT id, post_id, usuario_id, conteudo, data_criacao FROM comentario WHERE id = ?";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, comentarioId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new Comentario(
                            rs.getString("id"),
                            rs.getString("post_id"),
                            rs.getString("usuario_id"),
                            rs.getString("conteudo"),
                            rs.getTimestamp("data_criacao").toLocalDateTime()
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar comentario: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    public void deletarPost(String postId) {
        String sql = "DELETE FROM post WHERE id = ?";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, postId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar post: " + e.getMessage(), e);
        }
    }

    public void deletarComentario(String comentarioId) {
        String sql = "DELETE FROM comentario WHERE id = ?";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, comentarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deletar comentÃ¡rio: " + e.getMessage(), e);
        }
    }

    public void seguir(String seguidorId, String seguidoId) {
        String sql = "INSERT INTO seguidor (seguidor_id, seguido_id) VALUES (?, ?) ON CONFLICT DO NOTHING;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seguidorId);
            ps.setString(2, seguidoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao seguir usuÃ¡rio: " + e.getMessage(), e);
        }
    }

    public void deixarDeSeguir(String seguidorId, String seguidoId) {
        String sql = "DELETE FROM seguidor WHERE seguidor_id = ? AND seguido_id = ?;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seguidorId);
            ps.setString(2, seguidoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao deixar de seguir usuÃ¡rio: " + e.getMessage(), e);
        }
    }

    public boolean isSeguindo(String seguidorId, String seguidoId) {
        String sql = "SELECT 1 FROM seguidor WHERE seguidor_id = ? AND seguido_id = ?;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seguidorId);
            ps.setString(2, seguidoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar se segue usuÃ¡rio: " + e.getMessage(), e);
        }
    }

    public void criarPedidoSeguir(String seguidorId, String seguidoId) {
        String sql = "INSERT INTO pedido_seguir (seguidor_id, seguido_id) VALUES (?, ?) ON CONFLICT DO NOTHING;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seguidorId);
            ps.setString(2, seguidoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar pedido de seguir: " + e.getMessage(), e);
        }
    }

    public void removerPedidoSeguir(String seguidorId, String seguidoId) {
        String sql = "DELETE FROM pedido_seguir WHERE seguidor_id = ? AND seguido_id = ?;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seguidorId);
            ps.setString(2, seguidoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover pedido de seguir: " + e.getMessage(), e);
        }
    }

    public boolean isPedidoPendente(String seguidorId, String seguidoId) {
        String sql = "SELECT 1 FROM pedido_seguir WHERE seguidor_id = ? AND seguido_id = ?;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seguidorId);
            ps.setString(2, seguidoId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao verificar pedido pendente: " + e.getMessage(), e);
        }
    }

    public List<br.ufpb.dcx.projetos.login.models.Usuario> listarPedidosRecebidos(String seguidoId) {
        List<br.ufpb.dcx.projetos.login.models.Usuario> lista = new ArrayList<>();
        String sql = "SELECT u.id, u.nome, u.username, u.email, u.senha, u.tipo, u.bio, u.foto_url " +
                     "FROM usuario u " +
                     "JOIN pedido_seguir p ON u.id = p.seguidor_id " +
                     "WHERE p.seguido_id = ?;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, seguidoId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new br.ufpb.dcx.projetos.login.models.Usuario(
                            rs.getString("id"),
                            rs.getString("nome"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("senha"),
                            rs.getString("tipo"),
                            rs.getString("bio"),
                            rs.getString("foto_url")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar pedidos recebidos: " + e.getMessage(), e);
        }
        return lista;
    }

    public List<br.ufpb.dcx.projetos.login.models.Usuario> listarAmigos(String usuarioId) {
        List<br.ufpb.dcx.projetos.login.models.Usuario> lista = new ArrayList<>();
        String sql = "SELECT u.id, u.nome, u.username, u.email, u.senha, u.tipo, u.bio, u.foto_url " +
                     "FROM usuario u " +
                     "JOIN seguidor s1 ON u.id = s1.seguido_id " +
                     "JOIN seguidor s2 ON s1.seguido_id = s2.seguidor_id " +
                     "WHERE s1.seguidor_id = ? AND s2.seguido_id = ?;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioId);
            ps.setString(2, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new br.ufpb.dcx.projetos.login.models.Usuario(
                            rs.getString("id"),
                            rs.getString("nome"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("senha"),
                            rs.getString("tipo"),
                            rs.getString("bio"),
                            rs.getString("foto_url")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar amigos: " + e.getMessage(), e);
        }
        return lista;
    }

    public List<PostViewDTO> listarFeedPublico(String usuarioLogadoId) {
        List<PostViewDTO> feed = new ArrayList<>();
        String sql = "SELECT p.id, p.usuario_id, u.nome AS usuario_nome, u.username AS usuario_username, u.foto_url AS usuario_foto_url, " +
            "p.conteudo, p.data_criacao, p.tipo, p.musica_id, p.album_id, p.artista_id, " +
            "(SELECT COUNT(*) FROM post_estrela pe WHERE pe.post_id = p.id) AS qtd_estrelas, " +
            "(SELECT COUNT(*) FROM comentario c WHERE c.post_id = p.id) AS qtd_comentarios, " +
            "(SELECT COUNT(*) > 0 FROM post_estrela pe2 WHERE pe2.post_id = p.id AND pe2.usuario_id = ?) AS estrelado_por_mim, " +
            "m.titulo AS musica_titulo, m.artista AS musica_artista, m.nota AS musica_nota, m.youtube_url AS musica_youtube_url, m.capa_url AS musica_capa_url, " +
            "al.titulo AS album_titulo, al.artista AS album_artista, al.nota AS album_nota, al.capa_url AS album_capa_url, " +
            "ar.nome AS artista_nome, ar.capa_url AS artista_capa_url " +
            "FROM post p " +
            "JOIN usuario u ON p.usuario_id = u.id " +
            "LEFT JOIN musica m ON p.musica_id = m.id " +
            "LEFT JOIN album al ON p.album_id = al.id " +
            "LEFT JOIN artista ar ON p.artista_id = ar.id " +
            "ORDER BY p.data_criacao DESC LIMIT 30";

        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioLogadoId != null ? usuarioLogadoId : "");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    feed.add(mapPostView(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar feed público: " + e.getMessage(), e);
        }
        return feed;
    }

    public boolean temNovosPosts(String usuarioId, java.time.LocalDateTime desde) {
        String sql = "SELECT COUNT(*) FROM post p " +
                     "WHERE p.usuario_id IN (SELECT s1.seguido_id FROM seguidor s1 JOIN seguidor s2 ON s1.seguido_id = s2.seguidor_id WHERE s1.seguidor_id = ? AND s2.seguido_id = ?) " +
                     "AND p.data_criacao > ?;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioId);
            ps.setString(2, usuarioId);
            ps.setTimestamp(3, java.sql.Timestamp.valueOf(desde));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            // Ignorar falhas silenciosamente para resiliÃªncia do navbar
        }
        return false;
    }

    public List<br.ufpb.dcx.projetos.login.models.Usuario> listarSeguidores(String usuarioId) {
        List<br.ufpb.dcx.projetos.login.models.Usuario> lista = new ArrayList<>();
        String sql = "SELECT u.id, u.nome, u.username, u.email, u.senha, u.tipo, u.bio, u.foto_url " +
                     "FROM usuario u " +
                     "JOIN seguidor s ON u.id = s.seguidor_id " +
                     "WHERE s.seguido_id = ?;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new br.ufpb.dcx.projetos.login.models.Usuario(
                            rs.getString("id"),
                            rs.getString("nome"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("senha"),
                            rs.getString("tipo"),
                            rs.getString("bio"),
                            rs.getString("foto_url")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar seguidores: " + e.getMessage(), e);
        }
        return lista;
    }

    public List<br.ufpb.dcx.projetos.login.models.Usuario> listarSeguindo(String usuarioId) {
        List<br.ufpb.dcx.projetos.login.models.Usuario> lista = new ArrayList<>();
        String sql = "SELECT u.id, u.nome, u.username, u.email, u.senha, u.tipo, u.bio, u.foto_url " +
                     "FROM usuario u " +
                     "JOIN seguidor s ON u.id = s.seguido_id " +
                     "WHERE s.seguidor_id = ?;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(new br.ufpb.dcx.projetos.login.models.Usuario(
                            rs.getString("id"),
                            rs.getString("nome"),
                            rs.getString("username"),
                            rs.getString("email"),
                            rs.getString("senha"),
                            rs.getString("tipo"),
                            rs.getString("bio"),
                            rs.getString("foto_url")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar seguindo: " + e.getMessage(), e);
        }
        return lista;
    }

    private String formatarTempoAtras(java.time.LocalDateTime dateTime) {
        java.time.Duration d = java.time.Duration.between(dateTime, java.time.LocalDateTime.now());
        long segundos = d.getSeconds();
        if (segundos < 60) return "agora mesmo";
        long minutos = d.toMinutes();
        if (minutos < 60) return "há " + minutos + " min";
        long horas = d.toHours();
        if (horas < 24) return "há " + horas + " h";
        long dias = d.toDays();
        return "há " + dias + " dias";
    }

    public void criarNotificacao(String usuarioId, String autorId, String tipo, String postId, String conteudo) {
        String sql = "INSERT INTO notificacao (id, usuario_id, autor_id, tipo, post_id, conteudo, lida) VALUES (?, ?, ?, ?, ?, ?, false)";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, usuarioId);
            ps.setString(3, autorId);
            ps.setString(4, tipo);
            ps.setString(5, postId);
            ps.setString(6, conteudo);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao criar notificação: " + e.getMessage(), e);
        }
    }

    public List<NotificacaoViewDTO> listarNotificacoes(String usuarioId) {
        String sql = "SELECT n.id, n.usuario_id, n.autor_id, u.nome AS autor_nome, n.tipo, n.post_id, n.conteudo, n.lida, n.data_criacao, " +
                     "(SELECT COUNT(*) > 0 FROM seguidor s WHERE s.seguidor_id = n.usuario_id AND s.seguido_id = n.autor_id) AS seguindo_de_volta " +
                     "FROM notificacao n " +
                     "JOIN usuario u ON n.autor_id = u.id " +
                     "WHERE n.usuario_id = ? " +
                     "ORDER BY n.data_criacao DESC LIMIT 50";
        List<NotificacaoViewDTO> list = new ArrayList<>();
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(new NotificacaoViewDTO(
                        rs.getString("id"),
                        rs.getString("usuario_id"),
                        rs.getString("autor_id"),
                        rs.getString("autor_nome"),
                        rs.getString("tipo"),
                        rs.getString("post_id"),
                        rs.getString("conteudo"),
                        rs.getBoolean("lida"),
                        formatarTempoAtras(rs.getTimestamp("data_criacao").toLocalDateTime()),
                        rs.getBoolean("seguindo_de_volta")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar notificações: " + e.getMessage(), e);
        }
        return list;
    }

    public void marcarNotificacoesComoLidas(String usuarioId) {
        String sql = "UPDATE notificacao SET lida = true WHERE usuario_id = ?";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao marcar notificações como lidas: " + e.getMessage(), e);
        }
    }
}
