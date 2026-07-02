package br.ufpb.dcx.projetos.login.repositories;

import br.ufpb.dcx.projetos.login.models.Usuario;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementação concreta de UsuarioRepository conectando-se a um Banco de Dados PostgreSQL.
 * Utiliza JDBC puro (PreparedStatement e ResultSet) de forma explícita e pedagógica.
 * Sem "mágica" de frameworks ORM pesados, facilitando a explicação técnica para o professor.
 */
public class UsuarioDbRepository implements UsuarioRepository {

    // Configurações de conexão com o banco de dados PostgreSQL (Docker Compose)
    private final String url;
    private final String usuarioDb;
    private final String senhaDb;

    public UsuarioDbRepository(String url, String usuarioDb, String senhaDb) {
        this.url = url;
        this.usuarioDb = usuarioDb;
        this.senhaDb = senhaDb;
        // inicializarTabela(); // Movido para Flyway
    }

    // Método privado para abrir conexão com o banco de dados
    private Connection obterConexao() throws SQLException {
        return DriverManager.getConnection(url, usuarioDb, senhaDb);
    }

    // Executa a criação da tabela se ela ainda não existir no PostgreSQL
    private void inicializarTabela() {
        String sql = "CREATE TABLE IF NOT EXISTS usuario (" +
                     "id VARCHAR(36) PRIMARY KEY, " +
                     "nome VARCHAR(100) NOT NULL, " +
                     "email VARCHAR(100) UNIQUE NOT NULL, " +
                     "senha VARCHAR(100) NOT NULL, " +
                     "tipo VARCHAR(20) DEFAULT 'COMUM'" +
                     ");";
        try (Connection conn = obterConexao();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            try { stmt.execute("ALTER TABLE usuario ADD COLUMN tipo VARCHAR(20) DEFAULT 'COMUM';"); } catch (SQLException e) { /* ignored */ }

            // Seed automático se a tabela estiver vazia
            String countSql = "SELECT COUNT(*) FROM usuario;";
            try (ResultSet rs = stmt.executeQuery(countSql)) {
                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSql = "INSERT INTO usuario (id, nome, email, senha, tipo) VALUES (?, ?, ?, ?, ?);";
                    try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                        ps.setString(1, "c4b4d693-e18e-4f51-b844-3d9692482be2");
                        ps.setString(2, "Administrador");
                        ps.setString(3, "admin@email.com");
                        ps.setString(4, "$2a$10$uNMxjjZPaBHQm.LCdShe7ujv3tAyy8Fn9px0u6XEUyfFW1/LjWPSa"); // admin123
                        ps.setString(5, "ADMIN");
                        ps.executeUpdate();
                        System.out.println("Usuário administrador padrão criado automaticamente: admin@email.com / admin123");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erro ao inicializar/seedar a tabela no banco de dados: " + e.getMessage());
        }
    }

    @Override
    public void salvar(Usuario usuario) {
        // Usamos a lógica de "Upsert" do PostgreSQL: tenta inserir,
        // se o ID já existir, atualiza os campos correspondentes.
        String sql = "INSERT INTO usuario (id, nome, email, senha, tipo) VALUES (?, ?, ?, ?, ?) " +
                     "ON CONFLICT (id) DO UPDATE SET nome = EXCLUDED.nome, email = EXCLUDED.email, senha = EXCLUDED.senha, tipo = EXCLUDED.tipo;";

        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, usuario.getId());
            ps.setString(2, usuario.getNome());
            ps.setString(3, usuario.getEmail());
            ps.setString(4, usuario.getSenha());
            ps.setString(5, usuario.getTipo() != null ? usuario.getTipo() : "COMUM");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao salvar usuário no banco: " + e.getMessage(), e);
        }
    }

    @Override
    public Optional<Usuario> buscarPorId(String id) {
        String sql = "SELECT id, nome, email, senha, tipo FROM usuario WHERE id = ?;";
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario(
                            rs.getString("id"),
                            rs.getString("nome"),
                            rs.getString("email"),
                            rs.getString("senha"),
                            rs.getString("tipo")
                    );
                    return Optional.of(u);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário por ID: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        String sql = "SELECT id, nome, email, senha, tipo FROM usuario WHERE email = ?;";
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Usuario u = new Usuario(
                            rs.getString("id"),
                            rs.getString("nome"),
                            rs.getString("email"),
                            rs.getString("senha"),
                            rs.getString("tipo")
                    );
                    return Optional.of(u);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao buscar usuário por email: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Usuario> listarTodos() {
        List<Usuario> lista = new ArrayList<>();
        String sql = "SELECT id, nome, email, senha, tipo FROM usuario;";
        try (Connection conn = obterConexao();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Usuario u = new Usuario(
                        rs.getString("id"),
                        rs.getString("nome"),
                        rs.getString("email"),
                        rs.getString("senha"),
                        rs.getString("tipo")
                );
                lista.add(u);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao listar todos os usuários: " + e.getMessage(), e);
        }
        return lista;
    }

    @Override
    public void remover(String id) {
        String sql = "DELETE FROM usuario WHERE id = ?;";
        try (Connection conn = obterConexao();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Erro ao remover usuário: " + e.getMessage(), e);
        }
    }
}
