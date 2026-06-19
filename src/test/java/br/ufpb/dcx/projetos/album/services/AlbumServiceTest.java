package br.ufpb.dcx.projetos.album.services;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.repositories.AlbumRepository;
import br.ufpb.dcx.projetos.exceptions.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class AlbumServiceTest {

    private AlbumService service;
    private FakeAlbumRepository repo;

    @BeforeEach
    public void setup() {
        repo = new FakeAlbumRepository();
        service = new AlbumService(repo);
    }

    @Test
    public void testSaveAlbumComSucesso() {
        Album album = Album.novo("Samba Esquema Novo", "Jorge Ben", 1963);
        service.save(album);

        Optional<Album> cadastrado = service.findById(album.getId());
        assertTrue(cadastrado.isPresent());
        assertEquals("Samba Esquema Novo", cadastrado.get().getTitulo());
    }

    @Test
    public void testSaveAlbumTituloVazio() {
        Album album = Album.novo("", "Jorge Ben", 1963);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.save(album);
        });
        assertEquals("O título é obrigatório", exception.getMessage());
    }

    @Test
    public void testSaveAlbumArtistaVazio() {
        Album album = Album.novo("Samba Esquema Novo", "   ", 1963);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.save(album);
        });
        assertEquals("O artista é obrigatório", exception.getMessage());
    }

    @Test
    public void testSaveAlbumAnoInvalidoMenor() {
        Album album = Album.novo("Samba Esquema Novo", "Jorge Ben", 1899);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.save(album);
        });
        assertEquals("O ano de lançamento deve estar entre 1900 e 2100", exception.getMessage());
    }

    @Test
    public void testSaveAlbumAnoInvalidoMaior() {
        Album album = Album.novo("Samba Esquema Novo", "Jorge Ben", 2101);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.save(album);
        });
        assertEquals("O ano de lançamento deve estar entre 1900 e 2100", exception.getMessage());
    }

    @Test
    public void testUpdateAlbumComSucesso() {
        Album album = Album.novo("Samba Esquema Novo", "Jorge Ben", 1963);
        service.save(album);

        Album atualizacao = new Album(album.getId(), "A Tábua de Esmeralda", "Jorge Ben Jor", 1974);
        service.update(album.getId(), atualizacao);

        Optional<Album> alterado = service.findById(album.getId());
        assertTrue(alterado.isPresent());
        assertEquals("A Tábua de Esmeralda", alterado.get().getTitulo());
        assertEquals("Jorge Ben Jor", alterado.get().getArtista());
        assertEquals(1974, alterado.get().getAnoLancamento());
    }

    @Test
    public void testUpdateAlbumNaoEncontrado() {
        Album atualizacao = new Album("id-inexistente", "A Tábua de Esmeralda", "Jorge Ben Jor", 1974);
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.update("id-inexistente", atualizacao);
        });
        assertEquals("Album não encontrado com id: id-inexistente", exception.getMessage());
    }

    @Test
    public void testDeleteAlbumComSucesso() {
        Album album = Album.novo("Samba Esquema Novo", "Jorge Ben", 1963);
        service.save(album);

        service.deleteById(album.getId());
        assertTrue(service.findById(album.getId()).isEmpty());
    }

    @Test
    public void testDeleteAlbumNaoEncontrado() {
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.deleteById("id-qualquer");
        });
        assertEquals("Album não encontrado com id: id-qualquer", exception.getMessage());
    }

    private static class FakeAlbumRepository implements AlbumRepository {
        private final Map<String, Album> albums = new HashMap<>();

        @Override
        public void criar(Album album) {
            albums.put(album.getId(), album);
        }

        @Override
        public boolean atualizar(Album album) {
            if (albums.containsKey(album.getId())) {
                albums.put(album.getId(), album);
                return true;
            }
            return false;
        }

        @Override
        public Optional<Album> buscarPorId(String id) {
            return Optional.ofNullable(albums.get(id));
        }

        @Override
        public List<Album> listarTodos() {
            return new ArrayList<>(albums.values());
        }

        @Override
        public boolean remover(String id) {
            return albums.remove(id) != null;
        }
    }
}
