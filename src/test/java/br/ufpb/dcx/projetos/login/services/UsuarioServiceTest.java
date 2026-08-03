package br.ufpb.dcx.projetos.login.services;

import br.ufpb.dcx.projetos.login.models.Usuario;
import br.ufpb.dcx.projetos.login.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários para a classe UsuarioService.
 * Utiliza um repositório falso (Fake) em memória para testar as regras de validação 
 * de forma rápida e desacoplada do banco de dados relacional.
 */
public class UsuarioServiceTest {

    private UsuarioService service;
    private FakeUsuarioRepository repo;

    @BeforeEach
    public void setup() {
        repo = new FakeUsuarioRepository();
        service = new UsuarioService(repo, new PlaintextHashingStrategy());
    }

    @Test
    public void testCadastrarUsuarioComSucesso() {
        Usuario usuario = new Usuario("Maria Silva", "mariasilva", "maria@email.com", "senha123");
        service.cadastrarUsuario(usuario);

        Optional<Usuario> cadastrado = service.buscarPorEmail("maria@email.com");
        assertTrue(cadastrado.isPresent(), "O usuário deveria ter sido cadastrado com sucesso.");
        assertEquals("Maria Silva", cadastrado.get().getNome());
        assertEquals("mariasilva", cadastrado.get().getUsername());
    }

    @Test
    public void testCadastrarUsuarioNomeVazio() {
        Usuario usuario = new Usuario("", "mariasilva", "maria@email.com", "senha123");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.cadastrarUsuario(usuario);
        });
        assertEquals("Nome é obrigatório e não pode ser vazio.", exception.getMessage());
    }

    @Test
    public void testCadastrarUsuarioEmailInvalido() {
        Usuario usuario = new Usuario("Maria", "mariasilva", "maria_sem_arroba", "senha123");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.cadastrarUsuario(usuario);
        });
        assertEquals("Formato de e-mail inválido.", exception.getMessage());
    }

    @Test
    public void testCadastrarUsuarioSenhaCurta() {
        Usuario usuario = new Usuario("Maria", "mariasilva", "maria@email.com", "123");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.cadastrarUsuario(usuario);
        });
        assertEquals("A senha deve conter no mínimo 6 caracteres.", exception.getMessage());
    }

    @Test
    public void testCadastrarUsuarioEmailDuplicado() {
        Usuario usuario1 = new Usuario("Maria", "mariasilva", "maria@email.com", "senha123");
        service.cadastrarUsuario(usuario1);

        Usuario usuario2 = new Usuario("Maria Outra", "mariaoutra", "maria@email.com", "outrasenha");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.cadastrarUsuario(usuario2);
        });
        assertEquals("Já existe um usuário cadastrado com este e-mail.", exception.getMessage());
    }

    @Test
    public void testCadastrarUsuarioUsernameDuplicado() {
        Usuario usuario1 = new Usuario("Maria", "mariasilva", "maria@email.com", "senha123");
        service.cadastrarUsuario(usuario1);

        Usuario usuario2 = new Usuario("Maria Outra", "mariasilva", "maria2@email.com", "outrasenha");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.cadastrarUsuario(usuario2);
        });
        assertEquals("Já existe um usuário cadastrado com este username.", exception.getMessage());
    }

    /**
     * Repositório Fake (Falso) em memória que simula o banco de dados.
     * Permite rodar testes unitários instantâneos sem precisar que o Docker esteja rodando.
     */
    private static class FakeUsuarioRepository implements UsuarioRepository {
        private final Map<String, Usuario> usuarios = new HashMap<>();

        @Override
        public void salvar(Usuario usuario) {
            usuarios.put(usuario.getId(), usuario);
        }

        @Override
        public Optional<Usuario> buscarPorId(String id) {
            return Optional.ofNullable(usuarios.get(id));
        }

        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            return usuarios.values().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email.trim()))
                    .findFirst();
        }

        @Override
        public Optional<Usuario> buscarPorUsername(String username) {
            return usuarios.values().stream()
                    .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username.trim()))
                    .findFirst();
        }

        @Override
        public List<Usuario> listarTodos() {
            return new ArrayList<>(usuarios.values());
        }

        @Override
        public List<Usuario> buscar(String termo) {
            if (termo == null || termo.trim().isEmpty()) {
                return listarTodos();
            }
            return usuarios.values().stream()
                    .filter(u -> (u.getNome() != null && u.getNome().toLowerCase().contains(termo.toLowerCase().trim()))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(termo.toLowerCase().trim()))
                            || (u.getUsername() != null && u.getUsername().toLowerCase().contains(termo.toLowerCase().trim())))
                    .toList();
        }

        @Override
        public void remover(String id) {
            usuarios.remove(id);
        }
    }
}
