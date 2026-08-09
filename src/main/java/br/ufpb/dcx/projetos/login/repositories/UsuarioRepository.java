package br.ufpb.dcx.projetos.login.repositories;

import br.ufpb.dcx.projetos.login.models.Usuario;
import java.util.List;
import java.util.Optional;

/**
 * Interface que define o contrato para acesso aos dados do Usuário.
 * Representa a abstração exigida pelo princípio de desacoplamento.
 * Caso o mecanismo de persistência mude (de banco relacional para CSV ou vice-versa),
 * o resto do sistema continuará chamando esses mesmos métodos.
 */
public interface UsuarioRepository {

    // Grava um novo usuário ou atualiza um existente
    void salvar(Usuario usuario);

    // Busca um usuário pelo ID. Retorna um Optional para evitar erros de NullPointerException
    Optional<Usuario> buscarPorId(String id);

    // Busca um usuário pelo email (essencial para a tela de login)
    Optional<Usuario> buscarPorEmail(String email);

    // Busca um usuário pelo username
    Optional<Usuario> buscarPorUsername(String username);

    // Retorna a lista completa de usuários cadastrados
    List<Usuario> listarTodos();

    // Busca usuários por nome ou email (termo parcial)
    List<Usuario> buscar(String termo);

    // Remove um usuário pelo ID
    void remover(String id);

    default void salvarCodigoRecuperacao(String email, String codigo, java.time.LocalDateTime expiracao) {}

    default java.util.Optional<String> obterCodigoRecuperacaoValido(String email, String codigo) {
        return java.util.Optional.empty();
    }

    default void limparCodigoRecuperacao(String email) {}
}
