package br.ufpb.dcx.projetos.login.services;

import br.ufpb.dcx.projetos.login.models.Usuario;
import br.ufpb.dcx.projetos.login.repositories.UsuarioRepository;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Camada de Serviço (Service) para o domínio de Usuários.
 * Concentra as regras de negócio e validações da aplicação, impedindo dados inválidos de chegarem ao banco de dados.
 * Recebe o repositório pelo construtor (Injeção de Dependência manual).
 */
public class UsuarioService {

    private final UsuarioRepository repository;
    private final PasswordHashingStrategy hashingStrategy;

    public UsuarioService(UsuarioRepository repository, PasswordHashingStrategy hashingStrategy) {
        this.repository = repository;
        this.hashingStrategy = hashingStrategy;
    }

    /**
     * Valida as regras de negócio e cadastra um novo usuário no sistema.
     */
    public void cadastrarUsuario(Usuario usuario) {
        if (Objects.isNull(usuario)) {
            throw new IllegalArgumentException("Usuário não pode ser nulo.");
        }

        // 1. Validação do Nome
        if (Objects.isNull(usuario.getNome()) || usuario.getNome().trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório e não pode ser vazio.");
        }

        // 2. Validação do E-mail (Presença e Formato Básico)
        if (Objects.isNull(usuario.getEmail()) || usuario.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("E-mail é obrigatório.");
        }
        if (!usuario.getEmail().contains("@") || !usuario.getEmail().contains(".")) {
            throw new IllegalArgumentException("Formato de e-mail inválido.");
        }

        // 3. Validação de E-mail Duplicado (Regra de Negócio Crucial)
        Optional<Usuario> existente = repository.buscarPorEmail(usuario.getEmail().trim());
        if (existente.isPresent() && !existente.get().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail.");
        }

        // 4. Validação do tamanho da senha (mínimo de 6 caracteres)
        if (Objects.isNull(usuario.getSenha()) || usuario.getSenha().trim().isEmpty()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }
        if (usuario.getSenha().length() < 6) {
            throw new IllegalArgumentException("A senha deve conter no mínimo 6 caracteres.");
        }

        // Limpa espaços em branco extras do e-mail antes de salvar
        usuario.setEmail(usuario.getEmail().trim());

        // Criptografa a senha usando a estratégia ativa
        String senhaHashed = hashingStrategy.hash(usuario.getSenha());
        usuario.setSenha(senhaHashed);

        // Se todas as regras passarem, solicita a gravação ao repositório
        repository.salvar(usuario);
    }

    /**
     * Valida as credenciais de e-mail e senha de um usuário no sistema.
     * Utiliza a estratégia ativa para descriptografar/verificar o hash.
     */
    public boolean verificarCredenciais(String email, String senha) {
        if (email == null || senha == null || email.trim().isEmpty() || senha.trim().isEmpty()) {
            return false;
        }
        Optional<Usuario> usuarioOpt = repository.buscarPorEmail(email.trim());
        if (usuarioOpt.isEmpty()) {
            return false;
        }
        Usuario usuario = usuarioOpt.get();
        return hashingStrategy.verificar(senha, usuario.getSenha());
    }

    public Optional<Usuario> buscarPorId(String id) {
        if (Objects.isNull(id) || id.trim().isEmpty()) {
            return Optional.empty();
        }
        return repository.buscarPorId(id);
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        if (Objects.isNull(email) || email.trim().isEmpty()) {
            return Optional.empty();
        }
        return repository.buscarPorEmail(email.trim());
    }

    public List<Usuario> listarTodos() {
        return repository.listarTodos();
    }

    public void remover(String id) {
        if (Objects.isNull(id) || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID inválido para remoção.");
        }
        repository.remover(id);
    }
}
