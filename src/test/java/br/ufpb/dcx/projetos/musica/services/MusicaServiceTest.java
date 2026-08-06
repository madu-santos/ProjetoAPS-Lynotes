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
        Musica musica = Musica.novo("Mas Que Nada", "Jorge Ben", "Samba", 180, "Boa mÃºsica", 5.0, null, null, null, "user-1");
        service.save(musica);

        Optional<Musica> cadastrada = service.findById(musica.getId());
        assertTrue(cadastrada.isPresent());
        assertEquals("Mas Que Nada", cadastrada.get().getTitulo());
        assertEquals("Jorge Ben", cadastrada.get().getArtista());
        assertNull(cadastrada.get().getAlbumId());
        assertEquals(5, cadastrada.get().getNota());
    }

    @Test
    public void testSaveMusicaComSucessoComAlbum() {
        Album album = Album.novo("Samba Esquema Novo", "Jorge Ben", 1963, "user-1");
        albumRepo.criar(album);

        Musica musica = Musica.novo("Mas Que Nada", "Jorge Ben", "Samba", 180, "Boa mÃºsica", 5.0, null, null, album.getId(), "user-1");
        service.save(musica);

        Optional<Musica> cadastrada = service.findById(musica.getId());
        assertTrue(cadastrada.isPresent());
        assertEquals("Mas Que Nada", cadastrada.get().getTitulo());
        assertEquals(album.getId(), cadastrada.get().getAlbumId());
    }    @Test
    public void testSaveMusicaTituloVazio() {
        Musica musica = Musica.novo("", "Jorge Ben", "Samba", 180, "Boa música", 5.0, null, null, null, "user-1");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.save(musica);
        });
        assertEquals("O título da música é obrigatório", exception.getMessage());
    }

    @Test
    public void testSaveMusicaArtistaVazio() {
        Musica musica = Musica.novo("Mas Que Nada", "   ", "Samba", 180, "Boa música", 5.0, null, null, null, "user-1");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.save(musica);
        });
        assertEquals("O artista é obrigatório", exception.getMessage());
    }

    @Test
    public void testSaveMusicaNotaInvalida() {
        Musica musica = Musica.novo("Mas Que Nada", "Jorge Ben", "Samba", 180, "Boa música", 6.0, null, null, null, "user-1");
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            service.save(musica);
        });
        assertEquals("A nota deve ser entre 1 e 5 estrelas", exception.getMessage());
    }

    @Test
    public void testSaveMusicaAlbumInexistente() {
        Musica musica = Musica.novo("Mas Que Nada", "Jorge Ben", "Samba", 180, "Boa música", 5.0, null, null, "album-fake-id", "user-1");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.save(musica);
        });
        assertEquals("Álbum não encontrado com id: album-fake-id", exception.getMessage());
    }

    @Test
    public void testUpdateMusicaComSucesso() {
        Musica musica = Musica.novo("Mas Que Nada", "Jorge Ben", "Samba", 180, "Boa música", 5.0, null, null, null, "user-1");
        service.save(musica);

        Musica atualizacao = new Musica(musica.getId(), "Chove Chuva", "Jorge Ben", "Bossa Nova", 200, "Excelente", 4.0, null, null, null, "user-1");
        service.update(musica.getId(), atualizacao);

        Optional<Musica> alterada = service.findById(musica.getId());
        assertTrue(alterada.isPresent());
        assertEquals("Chove Chuva", alterada.get().getTitulo());
        assertEquals("Bossa Nova", alterada.get().getGenero());
        assertEquals(200, alterada.get().getDuracaoSegundos());
        assertEquals(4, alterada.get().getNota());
    }

    @Test
    public void testUpdateMusicaNaoEncontrada() {
        Musica atualizacao = new Musica("id-invalido", "Chove Chuva", "Jorge Ben", "Bossa Nova", 200, "Excelente", 4.0, null, null, null, "user-1");
        Exception exception = assertThrows(ResourceNotFoundException.class, () -> {
            service.update("id-invalido", atualizacao);
        });
        assertEquals("Música não encontrada com id: id-invalido", exception.getMessage());
    }

    @Test
    public void testDeleteMusicaComSucesso() {
        Musica musica = Musica.novo("Mas Que Nada", "Jorge Ben", "Samba", 180, "Boa música", 5.0, null, null, null, "user-1");
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
        public List<Musica> buscarPorUsuarioId(String usuarioId) {
            List<Musica> lista = new ArrayList<>();
            for (Musica m : musicas.values()) {
                if (Objects.equals(m.getUsuarioId(), usuarioId)) {
                    lista.add(m);
                }
            }
            return lista;
        }

        @Override
        public boolean remover(String id) {
            return musicas.remove(id) != null;
        }

        @Override
        public List<Musica> buscar(String busca, String usuarioId) {
            return musicas.values().stream()
                    .filter(m -> Objects.equals(m.getUsuarioId(), usuarioId) && m.getTitulo().toLowerCase().contains(busca.toLowerCase()))
                    .toList();
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
        public Optional<Album> buscarPorId(String id, String usuarioId) {
            Album album = albums.get(id);
            if (album != null && Objects.equals(album.getUsuarioId(), usuarioId)) {
                return Optional.of(album);
            }
            return Optional.empty();
        }

        @Override
        public List<Album> listarTodos(String usuarioId) {
            List<Album> lista = new ArrayList<>();
            for (Album a : albums.values()) {
                if (Objects.equals(a.getUsuarioId(), usuarioId)) {
                    lista.add(a);
                }
            }
            return lista;
        }

        @Override
        public List<Album> listarTodosGlobal() {
            return new ArrayList<>(albums.values());
        }

        @Override
        public boolean remover(String id, String usuarioId) {
            Album album = albums.get(id);
            if (album != null && Objects.equals(album.getUsuarioId(), usuarioId)) {
                albums.remove(id);
                return true;
            }
            return false;
        }

        @Override
        public List<Album> buscar(String termo, String usuarioId) {
            return albums.values().stream()
                    .filter(a -> Objects.equals(a.getUsuarioId(), usuarioId) && a.getTitulo().toLowerCase().contains(termo.toLowerCase()))
                    .toList();
        }
    }
}
