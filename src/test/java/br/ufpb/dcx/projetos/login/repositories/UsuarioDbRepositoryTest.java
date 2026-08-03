package br.ufpb.dcx.projetos.login.repositories;

import br.ufpb.dcx.projetos.login.models.Usuario;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Teste de integração automatizado para o repositório de banco de dados.
 * Conecta-se à porta do Docker local e testa inserção, busca e remoção.
 * 
 * NOTA PEDAGÓGICA: O teste utiliza a assunção (assumeTrue) para verificar se o banco está online.
 * Se o Docker estiver desligado, o teste é pulado de forma silenciosa e informativa, sem quebrar a compilação do Maven.
 */
public class UsuarioDbRepositoryTest {

    private static final String URL = "jdbc:postgresql://localhost:5432/spotify_db";
    private static final String USER = "postgres";
    private static final String PASSWORD = "postgres";
    private static UsuarioRepository repo;

    @BeforeAll
    public static void setup() {
        boolean bancoOnline = false;
        try {
            // Tenta abrir e fechar uma conexão rápida com o banco do Docker
            DriverManager.getConnection(URL, USER, PASSWORD).close();
            bancoOnline = true;
            repo = new UsuarioDbRepository(URL, USER, PASSWORD);
        } catch (SQLException e) {
            System.out.println("AVISO: Banco de dados PostgreSQL no Docker está offline. Pulando testes de banco.");
        }
        // Se bancoOnline for falso, a linha abaixo faz o JUnit pular este teste de forma limpa
        assumeTrue(bancoOnline, "PostgreSQL está offline - Teste ignorado.");
    }

    @Test
    public void testSalvarEBuscarUsuarioComSucesso() {
        // 1. Cria um novo usuário fictício
        Usuario usuario = new Usuario("Maria Teste", "mariateste", "maria.teste@email.com", "hash_bcrypt_seguro");

        // 2. Salva no banco de dados do Docker
        repo.salvar(usuario);

        // 3. Busca o usuário recém-criado pelo email
        Optional<Usuario> buscadoOpt = repo.buscarPorEmail("maria.teste@email.com");

        // 4. Valida se os dados gravados correspondem ao esperado
        assertTrue(buscadoOpt.isPresent(), "O usuário deveria ter sido encontrado no banco.");
        Usuario buscado = buscadoOpt.get();
        assertEquals(usuario.getId(), buscado.getId());
        assertEquals("Maria Teste", buscado.getNome());
        assertEquals("hash_bcrypt_seguro", buscado.getSenha());

        // 5. Limpa a sujeira do banco de dados (exclui o registro temporário de teste)
        repo.remover(usuario.getId());

        // 6. Garante que foi removido
        Optional<Usuario> removido = repo.buscarPorId(usuario.getId());
        assertTrue(removido.isEmpty(), "O usuário de teste deveria ter sido removido do banco.");
    }
}
