package br.ufpb.dcx.projetos.artista.services;

import br.ufpb.dcx.projetos.artista.dto.ArtistaDTO;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaIdInvalidoException;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaNaoEncontradoException;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaValidacaoException;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.repositories.ArtistaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtistaServiceTest {

    private ArtistaService service;
    private FakeArtistaRepository repository;

    @BeforeEach
    void setup() {
        repository = new FakeArtistaRepository();
        service = new ArtistaService(repository, new ArtistaValidator());
    }

    @Test
    void deveCadastrarArtistaNormalizado() {
        Artista artista = service.cadastrar(
                new ArtistaDTO("  Liniker  ", " MPB ", " Cantora brasileira ", "12345678909", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", true),
                "usuario-id-1"
        );

        assertEquals("Liniker", artista.getNome());
        assertEquals("MPB", artista.getGeneroMusical());
        assertEquals(1, service.listar("", "usuario-id-1").size());
    }

    @Test
    void deveRetornarErroDeValidacaoParaNomeVazio() {
        ArtistaValidacaoException erro = assertThrows(
                ArtistaValidacaoException.class,
                () -> service.cadastrar(new ArtistaDTO(" ", "Rock", null, "12345678909", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", true), "usuario-id-1")
        );

        assertEquals("Nome do artista é obrigatório.", erro.getMessage());
    }

    @Test
    void devePermitirArtistasComMesmoNome() {
        service.cadastrar(new ArtistaDTO("Djavan", "MPB", null, "12345678909", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", false), "usuario-id-1");
        service.cadastrar(new ArtistaDTO("Djavan", "Jazz", "Outro artista", "11122233344", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", false), "usuario-id-1");

        assertEquals(2, service.listar("", "usuario-id-1").size());
    }

    @Test
    void deveAtualizarArtista() {
        Artista artista = service.cadastrar(
                new ArtistaDTO("BaianaSystem", "Rock", null, "12345678909", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", true),
                "usuario-id-1"
        );

        Artista atualizado = service.atualizar(
                artista.getId(),
                new ArtistaDTO("BaianaSystem", "Afro-rock", "Grupo musical brasileiro", "12345678909", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", true),
                "usuario-id-1"
        );

        assertEquals("Afro-rock", atualizado.getGeneroMusical());
        assertEquals("Grupo musical brasileiro", atualizado.getBiografia());
    }

    @Test
    void deveRetornarNaoEncontradoAoAtualizarIdAusente() {
        String id = "fce7e912-82d2-402f-bc19-4dba7b28361a";

        assertThrows(
                ArtistaNaoEncontradoException.class,
                () -> service.atualizar(id, new ArtistaDTO("Nome", null, null, "12345678909", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", true), "usuario-id-1")
        );
    }

    @Test
    void deveRetornarIdInvalido() {
        assertThrows(ArtistaIdInvalidoException.class, () -> service.obter("abc", "usuario-id-1"));
    }

    @Test
    void deveExcluirArtista() {
        Artista artista = service.cadastrar(new ArtistaDTO("Elis Regina", "MPB", null, "12345678909", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", true), "usuario-id-1");

        service.remover(artista.getId(), "usuario-id-1");

        assertThrows(
                ArtistaNaoEncontradoException.class,
                () -> service.obter(artista.getId(), "usuario-id-1")
        );
    }

    @Test
    void deveBuscarPorNomeGeneroOuBiografia() {
        service.cadastrar(new ArtistaDTO("Djavan", "MPB", "Cantor alagoano", "12345678909", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", false), "usuario-id-1");
        service.cadastrar(new ArtistaDTO("Nina Simone", "Jazz", "Cantora americana", "11122233344", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", false), "usuario-id-1");

        assertEquals(1, service.listar("djavan", "usuario-id-1").size());
        assertEquals(1, service.listar("jazz", "usuario-id-1").size());
        assertEquals(1, service.listar("alagoano", "usuario-id-1").size());
        assertEquals(2, service.listar(" ", "usuario-id-1").size());
    }

    private static class FakeArtistaRepository implements ArtistaRepository {

        private final Map<String, Artista> artistas = new LinkedHashMap<>();

        @Override
        public void criar(Artista artista) {
            artistas.put(artista.getId(), artista);
        }

        @Override
        public boolean atualizar(Artista artista) {
            if (!artistas.containsKey(artista.getId())) {
                return false;
            }
            artistas.put(artista.getId(), artista);
            return true;
        }

        @Override
        public Optional<Artista> buscarPorId(String id, String usuarioId) {
            Artista a = artistas.get(id);
            if (a != null && a.getUsuarioId().equals(usuarioId)) {
                return Optional.of(a);
            }
            return Optional.empty();
        }

        @Override
        public List<Artista> listarTodos(String usuarioId) {
            return artistas.values().stream().filter(a -> a.getUsuarioId().equals(usuarioId)).toList();
        }

        @Override
        public List<Artista> listarTodosGlobal() {
            return new ArrayList<>(artistas.values());
        }

        @Override
        public List<Artista> buscar(String termo, String usuarioId) {
            String busca = termo.toLowerCase();
            return artistas.values().stream()
                    .filter(a -> a.getUsuarioId().equals(usuarioId))
                    .filter(artista -> contem(artista.getNome(), busca)
                            || contem(artista.getGeneroMusical(), busca)
                            || contem(artista.getBiografia(), busca))
                    .toList();
        }

        @Override
        public boolean remover(String id, String usuarioId) {
            Artista a = artistas.get(id);
            if (a != null && a.getUsuarioId().equals(usuarioId)) {
                return artistas.remove(id) != null;
            }
            return false;
        }

        @Override
        public List<Artista> listarPendentes() {
            return artistas.values().stream()
                    .filter(a -> a.getStatusVerificacao() == br.ufpb.dcx.projetos.artista.models.StatusVerificacao.PENDENTE)
                    .toList();
        }

        @Override
        public boolean atualizarStatusVerificacao(String id, br.ufpb.dcx.projetos.artista.models.StatusVerificacao status) {
            Artista a = artistas.get(id);
            if (a != null) {
                Artista atualizado = new Artista(
                        a.getId(), a.getNome(), a.getGeneroMusical(), a.getBiografia(), a.getCpf(), a.getDataNascimento(),
                        a.getCep(), a.getLogradouro(), a.getNumero(), a.getBairro(), a.getCidade(), a.getUf(),
                        status, a.getUsuarioId()
                );
                artistas.put(id, atualizado);
                return true;
            }
            return false;
        }

        private boolean contem(String valor, String busca) {
            return valor != null && valor.toLowerCase().contains(busca);
        }
    }
}
