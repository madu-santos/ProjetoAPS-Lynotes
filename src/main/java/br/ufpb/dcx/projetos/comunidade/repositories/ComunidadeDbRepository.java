package br.ufpb.dcx.projetos.comunidade.repositories;

import br.ufpb.dcx.projetos.comunidade.models.Comentario;
import br.ufpb.dcx.projetos.comunidade.models.Post;
import br.ufpb.dcx.projetos.comunidade.views.ComentarioViewDTO;
import br.ufpb.dcx.projetos.comunidade.views.PostViewDTO;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ComunidadeDbRepository {

    private final ConnectionFactory factory;

    public ComunidadeDbRepository(ConnectionFactory factory) {
        this.factory = factory;
    }

    public void salvarPost(Post post) {
        String sql = "INSERT INTO post (id, usuario_id, conteudo, data_criacao) VALUES (?, ?, ?, ?) " +
                     "ON CONFLICT (id) DO UPDATE SET conteudo = EXCLUDED.conteudo;";
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, post.getId());
            ps.setString(2, post.getUsuarioId());
            ps.setString(3, post.getConteudo());
            ps.setTimestamp(4, Timestamp.valueOf(post.getDataCriacao()));
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
                            rs.getTimestamp("data_criacao").toLocalDateTime()
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
            throw new RuntimeException("Erro ao salvar comentário: " + e.getMessage(), e);
        }
    }

    public void darEstrela(String postId, String usuarioId) {
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
            } else {
                try (PreparedStatement insPs = conn.prepareStatement(insertSql)) {
                    insPs.setString(1, postId);
                    insPs.setString(2, usuarioId);
                    insPs.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao processar estrela no post: " + e.getMessage(), e);
        }
    }

    public List<PostViewDTO> listarFeed(String usuarioLogadoId, String filtroUsuarioId) {
        List<PostViewDTO> feed = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT p.id, p.usuario_id, u.nome AS usuario_nome, p.conteudo, p.data_criacao, " +
            "(SELECT COUNT(*) FROM post_estrela pe WHERE pe.post_id = p.id) AS qtd_estrelas, " +
            "(SELECT COUNT(*) FROM comentario c WHERE c.post_id = p.id) AS qtd_comentarios, " +
            "(SELECT COUNT(*) > 0 FROM post_estrela pe2 WHERE pe2.post_id = p.id AND pe2.usuario_id = ?) AS estrelado_por_mim " +
            "FROM post p " +
            "JOIN usuario u ON p.usuario_id = u.id "
        );
        
        if (filtroUsuarioId != null) {
            sql.append("WHERE p.usuario_id = ? ");
        }
        
        sql.append("ORDER BY p.data_criacao DESC");

        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            
            ps.setString(1, usuarioLogadoId != null ? usuarioLogadoId : "");
            
            if (filtroUsuarioId != null) {
                ps.setString(2, filtroUsuarioId);
            }
            
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    feed.add(new PostViewDTO(
                            rs.getString("id"),
                            rs.getString("usuario_id"),
                            rs.getString("usuario_nome"),
                            rs.getString("conteudo"),
                            rs.getTimestamp("data_criacao").toLocalDateTime(),
                            rs.getInt("qtd_estrelas"),
                            rs.getInt("qtd_comentarios"),
                            rs.getBoolean("estrelado_por_mim")
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar feed: " + e.getMessage(), e);
        }
        return feed;
    }

    public Optional<PostViewDTO> buscarPostView(String postId, String usuarioLogadoId) {
        String sql = "SELECT p.id, p.usuario_id, u.nome AS usuario_nome, p.conteudo, p.data_criacao, " +
            "(SELECT COUNT(*) FROM post_estrela pe WHERE pe.post_id = p.id) AS qtd_estrelas, " +
            "(SELECT COUNT(*) FROM comentario c WHERE c.post_id = p.id) AS qtd_comentarios, " +
            "(SELECT COUNT(*) > 0 FROM post_estrela pe2 WHERE pe2.post_id = p.id AND pe2.usuario_id = ?) AS estrelado_por_mim " +
            "FROM post p " +
            "JOIN usuario u ON p.usuario_id = u.id " +
            "WHERE p.id = ?";
            
        try (Connection conn = factory.abrir();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuarioLogadoId != null ? usuarioLogadoId : "");
            ps.setString(2, postId);
            
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(new PostViewDTO(
                            rs.getString("id"),
                            rs.getString("usuario_id"),
                            rs.getString("usuario_nome"),
                            rs.getString("conteudo"),
                            rs.getTimestamp("data_criacao").toLocalDateTime(),
                            rs.getInt("qtd_estrelas"),
                            rs.getInt("qtd_comentarios"),
                            rs.getBoolean("estrelado_por_mim")
                    ));
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
}
