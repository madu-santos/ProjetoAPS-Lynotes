package br.ufpb.dcx.projetos.artista.services;

import br.ufpb.dcx.projetos.artista.dto.ArtistaForm;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaNaoEncontradoException;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.repositories.ArtistaRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public final class ArtistaService implements ArtistaUseCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(ArtistaService.class);

    private final ArtistaRepository repository;
    private final ArtistaValidator validator;

    public ArtistaService(ArtistaRepository repository, ArtistaValidator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Override
    public List<Artista> listar(String busca) {
        String termo = Objects.toString(busca, "").trim();
        return termo.isEmpty() ? repository.listarTodos() : repository.buscar(termo);
    }

    @Override
    public Artista obter(String id) {
        String idValidado = validator.validarId(id);
        return repository.buscarPorId(idValidado)
                .orElseThrow(() -> new ArtistaNaoEncontradoException(idValidado));
    }

    @Override
    public Artista cadastrar(ArtistaForm formulario) {
        ArtistaForm dados = validator.validar(formulario);
        Artista artista = Artista.novo(dados.nome(), dados.generoMusical(), dados.biografia());
        repository.criar(artista);
        LOGGER.info("Artista cadastrado. id={} nome={}", artista.getId(), artista.getNome());
        return artista;
    }

    @Override
    public Artista atualizar(String id, ArtistaForm formulario) {
        String idValidado = validator.validarId(id);
        ArtistaForm dados = validator.validar(formulario);
        Artista artista = new Artista(
                idValidado,
                dados.nome(),
                dados.generoMusical(),
                dados.biografia()
        );

        if (!repository.atualizar(artista)) {
            throw new ArtistaNaoEncontradoException(idValidado);
        }

        LOGGER.info("Artista atualizado. id={} nome={}", artista.getId(), artista.getNome());
        return artista;
    }

    @Override
    public void remover(String id) {
        String idValidado = validator.validarId(id);
        if (!repository.remover(idValidado)) {
            throw new ArtistaNaoEncontradoException(idValidado);
        }
        LOGGER.info("Artista removido. id={}", idValidado);
    }
}
