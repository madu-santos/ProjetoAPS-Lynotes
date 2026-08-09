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

        // 2b. Validação do Username (Presença)
        if (Objects.isNull(usuario.getUsername()) || usuario.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username é obrigatório e não pode ser vazio.");
        }

        // 3. Validação de E-mail Duplicado (Regra de Negócio Crucial)
        Optional<Usuario> existente = repository.buscarPorEmail(usuario.getEmail().trim());
        if (existente.isPresent() && !existente.get().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este e-mail.");
        }

        // 3b. Validação de Username Duplicado
        Optional<Usuario> existenteUsername = repository.buscarPorUsername(usuario.getUsername().trim());
        if (existenteUsername.isPresent() && !existenteUsername.get().getId().equals(usuario.getId())) {
            throw new IllegalArgumentException("Já existe um usuário cadastrado com este username.");
        }

        // 4. Validação do tamanho da senha (mínimo de 6 caracteres)
        if (Objects.isNull(usuario.getSenha()) || usuario.getSenha().trim().isEmpty()) {
            throw new IllegalArgumentException("Senha é obrigatória.");
        }
        if (usuario.getSenha().length() < 6) {
            throw new IllegalArgumentException("A senha deve conter no mínimo 6 caracteres.");
        }

        // Limpa espaços em branco extras do e-mail e username antes de salvar
        usuario.setEmail(usuario.getEmail().trim());
        usuario.setUsername(usuario.getUsername().trim());

        // Criptografa a senha usando a estratégia ativa se não for um hash existente
        boolean jaEhHash = usuario.getSenha().startsWith("$2a$") || usuario.getSenha().startsWith("$2y$") || usuario.getSenha().startsWith("$2b$");
        if (!jaEhHash) {
            String senhaHashed = hashingStrategy.hash(usuario.getSenha());
            usuario.setSenha(senhaHashed);
        }

        // Se todas as regras passarem, solicita a gravação ao repositório
        repository.salvar(usuario);
    }

    /**
     * Valida as credenciais de e-mail/username e senha de um usuário no sistema.
     * Utiliza a estratégia ativa para descriptografar/verificar o hash.
     */
    public boolean verificarCredenciais(String identificador, String senha) {
        if (identificador == null || senha == null || identificador.trim().isEmpty() || senha.trim().isEmpty()) {
            return false;
        }
        Optional<Usuario> usuarioOpt = repository.buscarPorEmail(identificador.trim());
        if (usuarioOpt.isEmpty()) {
            usuarioOpt = repository.buscarPorUsername(identificador.trim());
        }
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

    public Optional<Usuario> buscarPorUsername(String username) {
        if (Objects.isNull(username) || username.trim().isEmpty()) {
            return Optional.empty();
        }
        return repository.buscarPorUsername(username.trim());
    }

    public List<Usuario> listarTodos() {
        return repository.listarTodos();
    }

    public List<Usuario> buscar(String termo) {
        if (termo == null || termo.trim().isEmpty()) {
            return listarTodos();
        }
        return repository.buscar(termo);
    }

    public void remover(String id) {
        if (Objects.isNull(id) || id.trim().isEmpty()) {
            throw new IllegalArgumentException("ID inválido para remoção.");
        }
        repository.remover(id);
    }

    public void atualizarPerfil(String id, String nome, String bio) {
        if (Objects.isNull(nome) || nome.trim().isEmpty()) {
            throw new IllegalArgumentException("Nome é obrigatório.");
        }
        Optional<Usuario> usuarioOpt = repository.buscarPorId(id);
        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }
        Usuario usuario = usuarioOpt.get();
        usuario.setNome(nome.trim());
        usuario.setBio(bio != null ? bio.trim() : "");
        repository.salvar(usuario);
    }

    public void solicitarRecuperacao(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("E-mail é obrigatório.");
        }
        Optional<Usuario> usuarioOpt = repository.buscarPorEmail(email.trim());
        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Nenhum usuário cadastrado com este e-mail.");
        }
        
        String codigo = String.format("%06d", new java.util.Random().nextInt(1000000));
        repository.salvarCodigoRecuperacao(email.trim(), codigo, java.time.LocalDateTime.now().plusMinutes(15));
        
        System.out.println("=================================================");
        System.out.println("SIMULACAO DE ENVIO DE E-MAIL (RECUPERACAO DE SENHA)");
        System.out.println("Para: " + email.trim());
        System.out.println("Seu codigo de verificacao eh: " + codigo);
        System.out.println("Este codigo expira em 15 minutos.");
        System.out.println("=================================================");
    }

    public void confirmarRecuperacao(String email, String codigo, String novaSenha) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("E-mail é obrigatório.");
        }
        if (codigo == null || codigo.trim().isEmpty()) {
            throw new IllegalArgumentException("Código de verificação é obrigatório.");
        }
        if (novaSenha == null || novaSenha.trim().isEmpty()) {
            throw new IllegalArgumentException("Nova senha é obrigatória.");
        }
        if (novaSenha.length() < 6) {
            throw new IllegalArgumentException("A nova senha deve conter no mínimo 6 caracteres.");
        }

        Optional<String> codigoValido = repository.obterCodigoRecuperacaoValido(email.trim(), codigo.trim());
        if (codigoValido.isEmpty()) {
            throw new IllegalArgumentException("Código de recuperação inválido ou expirado.");
        }

        Optional<Usuario> usuarioOpt = repository.buscarPorEmail(email.trim());
        if (usuarioOpt.isEmpty()) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        Usuario usuario = usuarioOpt.get();
        usuario.setSenha(hashingStrategy.hash(novaSenha));
        repository.salvar(usuario);
        
        repository.limparCodigoRecuperacao(email.trim());
    }
}
