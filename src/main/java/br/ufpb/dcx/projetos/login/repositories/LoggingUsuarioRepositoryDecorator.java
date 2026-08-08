package br.ufpb.dcx.projetos.login.repositories;

import br.ufpb.dcx.projetos.login.models.Usuario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class LoggingUsuarioRepositoryDecorator implements UsuarioRepository {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingUsuarioRepositoryDecorator.class);
    private final UsuarioRepository decorado;

    public LoggingUsuarioRepositoryDecorator(UsuarioRepository decorado) {
        this.decorado = decorado;
    }

    @Override
    public void salvar(Usuario usuario) {
        long inicio = System.currentTimeMillis();
        decorado.salvar(usuario);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): salvar usuario executado em {}ms. id={} email={}", 
                (fim - inicio), usuario.getId(), usuario.getEmail());
    }

    @Override
    public Optional<Usuario> buscarPorId(String id) {
        long inicio = System.currentTimeMillis();
        Optional<Usuario> resultado = decorado.buscarPorId(id);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: buscarPorId executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public Optional<Usuario> buscarPorEmail(String email) {
        long inicio = System.currentTimeMillis();
        Optional<Usuario> resultado = decorado.buscarPorEmail(email);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: buscarPorEmail executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public Optional<Usuario> buscarPorUsername(String username) {
        long inicio = System.currentTimeMillis();
        Optional<Usuario> resultado = decorado.buscarPorUsername(username);
        long fim = System.currentTimeMillis();
        LOGGER.debug("Decorator: buscarPorUsername executado em {}ms", (fim - inicio));
        return resultado;
    }

    @Override
    public List<Usuario> listarTodos() {
        return decorado.listarTodos();
    }

    @Override
    public List<Usuario> buscar(String termo) {
        return decorado.buscar(termo);
    }

    @Override
    public void remover(String id) {
        long inicio = System.currentTimeMillis();
        decorado.remover(id);
        long fim = System.currentTimeMillis();
        LOGGER.info("AUDITORIA/PERFORMANCE (Decorator): remover usuario executado em {}ms. id={}", 
                (fim - inicio), id);
    }

    @Override
    public void salvarCodigoRecuperacao(String email, String codigo, java.time.LocalDateTime expiracao) {
        decorado.salvarCodigoRecuperacao(email, codigo, expiracao);
    }

    @Override
    public java.util.Optional<String> obterCodigoRecuperacaoValido(String email, String codigo) {
        return decorado.obterCodigoRecuperacaoValido(email, codigo);
    }

    @Override
    public void limparCodigoRecuperacao(String email) {
        decorado.limparCodigoRecuperacao(email);
    }
}
