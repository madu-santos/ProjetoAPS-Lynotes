package br.ufpb.dcx.projetos.artista.services;

import br.ufpb.dcx.projetos.artista.dto.ArtistaDTO;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaNaoEncontradoException;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.models.StatusVerificacao;
import br.ufpb.dcx.projetos.artista.repositories.ArtistaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class ArtistaService implements ArtistaUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtistaService.class);

    private final ArtistaRepository repository;
    private final ArtistaValidator validator;

    public ArtistaService(ArtistaRepository repository, ArtistaValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Override
    public List<Artista> listar(String busca, String usuarioId) {
        String termo = Objects.toString(busca, "").trim();
        return termo.isEmpty() ? repository.listarTodos(usuarioId) : repository.buscar(termo, usuarioId);
    }
    
    public List<Artista> findAllGlobal() {
        return repository.listarTodosGlobal();
    }

    @Override
    public Artista obter(String id, String usuarioId) {
        String idValidado = validator.validarId(id);
        return repository.buscarPorId(idValidado, usuarioId)
                .orElseThrow(() -> new ArtistaNaoEncontradoException(idValidado));
    }

    @Override
    public Artista cadastrar(ArtistaDTO formulario, String usuarioId) {
        ArtistaDTO dados = validator.validar(formulario);
        StatusVerificacao status = Boolean.TRUE.equals(dados.solicitarVerificacao()) ? StatusVerificacao.PENDENTE : StatusVerificacao.NAO_SOLICITADO;

        if (dados.youtubeId() != null && !dados.youtubeId().isBlank()) {
            var existente = repository.buscarPorYouTubeId(dados.youtubeId());
            if (existente.isPresent() && existente.get().getUsuarioId().equals(usuarioId)) {
                Artista a = existente.get();
                Artista atualizado = new Artista(
                        a.getId(),
                        dados.nome(),
                        dados.generoMusical(),
                        dados.biografia(),
                        dados.cpf(),
                        dados.dataNascimento(),
                        dados.cep(),
                        dados.logradouro(),
                        dados.numero(),
                        dados.bairro(),
                        dados.cidade(),
                        dados.uf(),
                        status,
                        dados.nota(),
                        usuarioId,
                        dados.youtubeId(),
                        dados.capaUrl()
                );
                repository.atualizar(atualizado);
                return atualizado;
            }
        }

        Artista artista = Artista.novo(
                dados.nome(),
                dados.generoMusical(),
                dados.biografia(),
                dados.cpf(),
                dados.dataNascimento(),
                dados.cep(),
                dados.logradouro(),
                dados.numero(),
                dados.bairro(),
                dados.cidade(),
                dados.uf(),
                status,
                dados.nota(),
                usuarioId,
                dados.youtubeId(),
                dados.capaUrl()
        );
        repository.criar(artista);
        LOGGER.info("Artista cadastrado. id={} nome={} usuarioId={}", artista.getId(), artista.getNome(), usuarioId);
        return artista;
    }

    @Override
    public Artista atualizar(String id, ArtistaDTO formulario, String usuarioId) {
        String idValidado = validator.validarId(id);
        ArtistaDTO dados = validator.validar(formulario);
        // Mantenha o status original por enquanto (a aprovação será feita pelo admin)
        Artista artistaOriginal = obter(id, usuarioId);
        StatusVerificacao status = artistaOriginal.getStatusVerificacao();
        
        // Se o usuário tentar solicitar verificação de um não solicitado
        if (Boolean.TRUE.equals(dados.solicitarVerificacao()) && status == StatusVerificacao.NAO_SOLICITADO) {
            status = StatusVerificacao.PENDENTE;
        }

        String yid = dados.youtubeId() != null ? dados.youtubeId() : artistaOriginal.getYoutubeId();
        String capa = dados.capaUrl() != null ? dados.capaUrl() : artistaOriginal.getCapaUrl();

        Artista artista = new Artista(
                idValidado,
                dados.nome(),
                dados.generoMusical(),
                dados.biografia(),
                dados.cpf(),
                dados.dataNascimento(),
                dados.cep(),
                dados.logradouro(),
                dados.numero(),
                dados.bairro(),
                dados.cidade(),
                dados.uf(),
                status,
                dados.nota(),
                usuarioId,
                yid,
                capa
        );

        if (!repository.atualizar(artista)) {
            throw new ArtistaNaoEncontradoException(idValidado);
        }

        LOGGER.info("Artista atualizado. id={} nome={}", artista.getId(), artista.getNome());
        return artista;
    }

    @Override
    public void remover(String id, String usuarioId) {
        String idValidado = validator.validarId(id);
        if (!repository.remover(idValidado, usuarioId)) {
            throw new ArtistaNaoEncontradoException(idValidado);
        }
        LOGGER.info("Artista removido. id={}", idValidado);
    }

    public List<Artista> listarPendentes() {
        return repository.listarPendentes();
    }

    public void aprovar(String id) {
        String idValidado = validator.validarId(id);
        if (!repository.atualizarStatusVerificacao(idValidado, StatusVerificacao.APROVADO)) {
            throw new ArtistaNaoEncontradoException(idValidado);
        }
        LOGGER.info("Artista aprovado pelo Admin. id={}", idValidado);
    }

    public void rejeitar(String id) {
        String idValidado = validator.validarId(id);
        if (!repository.atualizarStatusVerificacao(idValidado, StatusVerificacao.REJEITADO)) {
            throw new ArtistaNaoEncontradoException(idValidado);
        }
        LOGGER.info("Artista rejeitado pelo Admin. id={}", idValidado);
    }
}
