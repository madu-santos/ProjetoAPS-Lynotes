package br.ufpb.dcx.projetos.musica.services;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.repositories.AlbumRepository;
import br.ufpb.dcx.projetos.exceptions.ResourceNotFoundException;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.repositories.MusicaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class MusicaServiceTest {

    private MusicaService service;
    private FakeMusicaRepository repo;
    private FakeAlbumRepository albumRepo;

    @BeforeEach
    public void setup() {
        repo = new FakeMusicaRepository();
        albumRepo = new FakeAlbumRepository();
        service = new MusicaService(repo, albumRepo);
    }

    @Test
    public void testSaveMusicaComSucessoSemAlbum() {
        Musica musica = Musica.novo("Mas Que Nada", "Samba", 180, null);
        service.save(musica);

        Optional<Musica> cadastrada = service.findById(musica.getId());
        assertTrue(cadastrada.isPresent());
        assertEquals("Mas Que Nada", cadastrada.get().getTitulo());
        assertNull(cadastrada.get().getAlbumId());
    }

    @Test
    public void testSaveMusicaComSucessoComAlbum() {
        Album album = Album.novo("Samba Esquema Novo", "Jorge Ben", 1963);
        albumRepo.criar(album);

        Musica musica = Musica.novo("Mas Que Nada", "Samba", 180, album.getId());
        service.save(musica);

        Optional<Musica> cadastrada = service.findById(musica.getId());
        assertTrue(cadastrada.isPresent());
        assertEquals("Mas Que Nada", cadastrada.get().getTitulo());
        assertEquals(album.getId(), cadastrada.get().getAlbumId());
    }

    @Test
    public void testSaveMusicaTituloVazio() {
        Musica musica = Musica.novo("", "Samba", 180, null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.save(musica);
        });
        assertEquals("O título da música é obrigatório", exception.getMessage());
    }

    @Test
    public void testSaveMusicaGeneroVazio() {
        Musica musica = Musica.novo("Mas Que Nada", "   ", 180, null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.save(musica);
        });
        assertEquals("O gênero da música é obrigatório", exception.getMessage());
    }

    @Test
    public void testSaveMusicaDuracaoInvalida() {
        Musica musica = Musica.novo("Mas Que Nada", "Samba", 0, null);
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.save(musica);
        });
        assertEquals("A duração da música deve ser de pelo menos 1 segundo", exception.getMessage());
    }

    @Test
    public void testSaveMusicaAlbumInexistente() {
        Musica musica = Musica.novo("Mas Que Nada", "Samba", 180, "album-fake-id");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.save(musica);
        });
        assertEquals("Álbum não encontrado com id: album-fake-id", exception.getMessage());
    }

    @Test
    public void testUpdateMusicaComSucesso() {
        Musica musica = Musica.novo("Mas Que Nada", "Samba", 180, null);
        service.save(musica);

        Musica atualizacao = new Musica(musica.getId(), "Chove Chuva", "Bossa Nova", 200, null);
        service.update(musica.getId(), atualizacao);

        Optional<Musica> alterada = service.findById(musica.getId());
        assertTrue(alterada.isPresent());
        assertEquals("Chove Chuva", alterada.get().getTitulo());
        assertEquals("Bossa Nova", alterada.get().getGenero());
        assertEquals(200, alterada.get().getDuracaoSegundos());
    }

    @Test
    public void testUpdateMusicaNaoEncontrada() {
        Musica atualizacao = new Musica("id-invalido", "Chove Chuva", "Bossa Nova", 200, null);
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.update("id-invalido", atualizacao);
        });
        assertEquals("Música não encontrada com id: id-invalido", exception.getMessage());
    }

    @Test
    public void testDeleteMusicaComSucesso() {
        Musica musica = Musica.novo("Mas Que Nada", "Samba", 180, null);
        service.save(musica);

        service.deleteById(musica.getId());
        assertTrue(service.findById(musica.getId()).isEmpty());
    }

    @Test
    public void testDeleteMusicaNaoEncontrada() {
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.deleteById("id-invalido");
        });
        assertEquals("Música não encontrada com id: id-invalido", exception.getMessage());
    }

    private static class FakeMusicaRepository implements MusicaRepository {
        private final Map<String, Musica> musicas = new HashMap<>();

        @Override
        public void criar(Musica musica) {
            musicas.put(musica.getId(), musica);
        }

        @Override
        public boolean atualizar(Musica musica) {
            if (musicas.containsKey(musica.getId())) {
                musicas.put(musica.getId(), musica);
                return true;
            }
            return false;
        }

        @Override
        public Optional<Musica> buscarPorId(String id) {
            return Optional.ofNullable(musicas.get(id));
        }

        @Override
        public List<Musica> listarTodas() {
            return new ArrayList<>(musicas.values());
        }

        @Override
        public List<Musica> buscarPorAlbumId(String albumId) {
            List<Musica> lista = new ArrayList<>();
            for (Musica m : musicas.values()) {
                if (Objects.equals(m.getAlbumId(), albumId)) {
                    lista.add(m);
                }
            }
            return lista;
        }

        @Override
        public boolean remover(String id) {
            return musicas.remove(id) != null;
        }
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
