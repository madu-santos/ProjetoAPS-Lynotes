package br.ufpb.dcx.projetos.artista.services;

import br.ufpb.dcx.projetos.artista.dto.ArtistaForm;
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
                new ArtistaForm("  Liniker  ", " MPB ", " Cantora brasileira ")
        );

        assertEquals("Liniker", artista.getNome());
        assertEquals("MPB", artista.getGeneroMusical());
        assertEquals(1, service.listar("").size());
    }

    @Test
    void deveRetornarErroDeValidacaoParaNomeVazio() {
        ArtistaValidacaoException erro = assertThrows(
                ArtistaValidacaoException.class,
                () -> service.cadastrar(new ArtistaForm(" ", "Rock", null))
        );

        assertEquals("Nome do artista é obrigatório.", erro.getMessage());
    }

    @Test
    void devePermitirArtistasComMesmoNome() {
        service.cadastrar(new ArtistaForm("Djavan", "MPB", null));
        service.cadastrar(new ArtistaForm("Djavan", "Jazz", "Outro artista"));

        assertEquals(2, service.listar("").size());
    }

    @Test
    void deveAtualizarArtista() {
        Artista artista = service.cadastrar(
                new ArtistaForm("BaianaSystem", "Rock", null)
        );

        Artista atualizado = service.atualizar(
                artista.getId(),
                new ArtistaForm("BaianaSystem", "Afro-rock", "Grupo musical brasileiro")
        );

        assertEquals("Afro-rock", atualizado.getGeneroMusical());
        assertEquals("Grupo musical brasileiro", atualizado.getBiografia());
    }

    @Test
    void deveRetornarNaoEncontradoAoAtualizarIdAusente() {
        String id = "fce7e912-82d2-402f-bc19-4dba7b28361a";

        assertThrows(
                ArtistaNaoEncontradoException.class,
                () -> service.atualizar(id, new ArtistaForm("Nome", null, null))
        );
    }

    @Test
    void deveRetornarIdInvalido() {
        assertThrows(ArtistaIdInvalidoException.class, () -> service.obter("abc"));
    }

    @Test
    void deveExcluirArtista() {
        Artista artista = service.cadastrar(new ArtistaForm("Elis Regina", "MPB", null));

        service.remover(artista.getId());

        assertThrows(
                ArtistaNaoEncontradoException.class,
                () -> service.obter(artista.getId())
        );
    }

    @Test
    void deveBuscarPorNomeGeneroOuBiografia() {
        service.cadastrar(new ArtistaForm("Djavan", "MPB", "Cantor alagoano"));
        service.cadastrar(new ArtistaForm("Nina Simone", "Jazz", "Cantora americana"));

        assertEquals(1, service.listar("djavan").size());
        assertEquals(1, service.listar("jazz").size());
        assertEquals(1, service.listar("alagoano").size());
        assertEquals(2, service.listar(" ").size());
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
        public Optional<Artista> buscarPorId(String id) {
            return Optional.ofNullable(artistas.get(id));
        }

        @Override
        public List<Artista> listarTodos() {
            return new ArrayList<>(artistas.values());
        }

        @Override
        public List<Artista> buscar(String termo) {
            String busca = termo.toLowerCase();
            return artistas.values().stream()
                    .filter(artista -> contem(artista.getNome(), busca)
                            || contem(artista.getGeneroMusical(), busca)
                            || contem(artista.getBiografia(), busca))
                    .toList();
        }

        @Override
        public boolean remover(String id) {
            return artistas.remove(id) != null;
        }

        private boolean contem(String valor, String busca) {
            return valor != null && valor.toLowerCase().contains(busca);
        }
    }
}
