package br.ufpb.dcx.projetos.musica.services;

import br.ufpb.dcx.projetos.album.repositories.AlbumRepository;
import br.ufpb.dcx.projetos.exceptions.ResourceNotFoundException;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.repositories.MusicaRepository;

import java.util.List;
import java.util.Optional;

public class MusicaService {

    private final MusicaRepository musicaRepository;
    private final AlbumRepository albumRepository;

    public MusicaService(MusicaRepository musicaRepository, AlbumRepository albumRepository) {
        this.musicaRepository = musicaRepository;
        this.albumRepository = albumRepository;
    }

    public Musica save(Musica musica) {
        validar(musica.getTitulo(), musica.getArtista(), musica.getNota());

        // Garantir que o álbum associado existe no banco
        if (musica.getAlbumId() != null && !musica.getAlbumId().isBlank()) {
            albumRepository.buscarPorId(musica.getAlbumId(), musica.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Álbum não encontrado com id: " + musica.getAlbumId()));
        }

        // Deduplicação: se já existir música do mesmo usuário com este youtubeId, reaproveita ou atualiza
        if (musica.getYoutubeId() != null && !musica.getYoutubeId().isBlank()) {
            Optional<Musica> existente = musicaRepository.buscarPorYouTubeId(musica.getYoutubeId());
            if (existente.isPresent() && existente.get().getUsuarioId().equals(musica.getUsuarioId())) {
                Musica m = existente.get();
                Musica atualizada = new Musica(
                        m.getId(),
                        musica.getTitulo(),
                        musica.getArtista(),
                        musica.getGenero(),
                        musica.getDuracaoSegundos(),
                        musica.getResenha(),
                        musica.getNota(),
                        musica.getSpotifyUrl(),
                        musica.getYoutubeUrl(),
                        musica.getAlbumId(),
                        musica.getUsuarioId(),
                        musica.getYoutubeId(),
                        musica.getCapaUrl(),
                        musica.isOcultaDaBiblioteca()
                );
                musicaRepository.atualizar(atualizada);
                return atualizada;
            }
        }

        if (musicaRepository.buscarPorId(musica.getId()).isPresent()) {
            musicaRepository.atualizar(musica);
        } else {
            musicaRepository.criar(musica);
        }
        return musica;
    }

    public Optional<Musica> findByYouTubeId(String youtubeId) {
        return musicaRepository.buscarPorYouTubeId(youtubeId);
    }

    public List<Musica> findAll() {
        return musicaRepository.listarTodas();
    }

    public List<Musica> findByUsuarioId(String usuarioId) {
        return musicaRepository.buscarPorUsuarioId(usuarioId);
    }

    public List<Musica> search(String termo, String usuarioId) {
        return musicaRepository.buscar(termo, usuarioId);
    }

    public List<Musica> findByAlbumId(String albumId) {
        return musicaRepository.buscarPorAlbumId(albumId);
    }

    public void importarFaixasAlbumItunes(String collectionId, String albumId, String usuarioId, String capaUrl) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                String url = "https://itunes.apple.com/lookup?id=" + collectionId + "&entity=song";
                java.net.http.HttpClient httpClient = java.net.http.HttpClient.newHttpClient();
                java.net.http.HttpRequest req = java.net.http.HttpRequest.newBuilder()
                        .uri(java.net.URI.create(url))
                        .timeout(java.time.Duration.ofSeconds(10))
                        .GET()
                        .build();
                java.net.http.HttpResponse<String> res = httpClient.send(req, java.net.http.HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(res.body());
                    com.fasterxml.jackson.databind.JsonNode results = root.path("results");
                    for (com.fasterxml.jackson.databind.JsonNode item : results) {
                        if ("track".equals(item.path("wrapperType").asText(""))) {
                            int trackTimeMillis = item.path("trackTimeMillis").asInt(0);
                            int duracaoSegundos = trackTimeMillis / 1000;
                            String trackId = item.path("trackId").asText("");
                            
                            Musica faixa = Musica.novo(
                                item.path("trackName").asText(""),
                                item.path("artistName").asText(""),
                                item.path("primaryGenreName").asText(""),
                                duracaoSegundos > 0 ? duracaoSegundos : null,
                                "",
                                null,
                                null,
                                null,
                                albumId,
                                usuarioId,
                                "itunes_track_" + trackId,
                                capaUrl
                            );
                            save(faixa);
                        }
                    }
                }
            } catch (Exception e) {
                // Ignore erros de importação para não travar o log
            }
        });
    }

    public void importarFaixasAlbumYouTube(String playlistId, String albumId, String usuarioId, String capaUrl) {
        java.util.concurrent.CompletableFuture.runAsync(() -> {
            try {
                br.ufpb.dcx.projetos.infra.youtube.YouTubeService ytService = new br.ufpb.dcx.projetos.infra.youtube.YouTubeService();
                List<br.ufpb.dcx.projetos.infra.youtube.dto.YouTubeItemDTO> itens = ytService.obterItensPlaylist(playlistId);
                for (var item : itens) {
                    Musica faixa = Musica.novo(
                        item.titulo(),
                        item.artistaOuCanal(),
                        "Pop",
                        item.duracaoSegundos(),
                        "",
                        null,
                        null,
                        null,
                        albumId,
                        usuarioId,
                        item.youtubeId(),
                        capaUrl
                    );
                    save(faixa);
                }
            } catch (Exception e) {
                // Ignore erros de importação
            }
        });
    }

    public Optional<Musica> findById(String id) {
        return musicaRepository.buscarPorId(id);
    }

    public Musica update(String id, Musica musicaDetails) {
        Musica musica = musicaRepository.buscarPorId(id)
                .orElseThrow(() -> new ResourceNotFoundException("Música não encontrada com id: " + id));

        validar(musicaDetails.getTitulo(), musicaDetails.getArtista(), musicaDetails.getNota());

        // Garantir que o álbum associado existe no banco se fornecido
        if (musicaDetails.getAlbumId() != null && !musicaDetails.getAlbumId().isBlank()) {
            albumRepository.buscarPorId(musicaDetails.getAlbumId(), musicaDetails.getUsuarioId())
                    .orElseThrow(() -> new ResourceNotFoundException("Álbum não encontrado com id: " + musicaDetails.getAlbumId()));
        }

        Musica musicaAtualizada = new Musica(
                musica.getId(),
                musicaDetails.getTitulo(),
                musicaDetails.getArtista(),
                musicaDetails.getGenero(),
                musicaDetails.getDuracaoSegundos(),
                musicaDetails.getResenha(),
                musicaDetails.getNota(),
                musicaDetails.getSpotifyUrl(),
                musicaDetails.getYoutubeUrl(),
                musicaDetails.getAlbumId(),
                musicaDetails.getUsuarioId(),
                musicaDetails.getYoutubeId() != null ? musicaDetails.getYoutubeId() : musica.getYoutubeId(),
                musicaDetails.getCapaUrl() != null ? musicaDetails.getCapaUrl() : musica.getCapaUrl(),
                musicaDetails.isOcultaDaBiblioteca()
        );
        musicaRepository.atualizar(musicaAtualizada);
        return musicaAtualizada;
    }

    public void deleteById(String id) {
        if (musicaRepository.buscarPorId(id).isEmpty()) {
            throw new ResourceNotFoundException("Música não encontrada com id: " + id);
        }
        musicaRepository.remover(id);
    }

    public boolean alternarOculta(String id, String usuarioId) {
        return musicaRepository.alternarOculta(id, usuarioId);
    }

    private void validar(String titulo, String artista, Double nota) {
        if (titulo == null || titulo.trim().isEmpty()) {
            throw new IllegalArgumentException("O título da música é obrigatório");
        }
        if (artista == null || artista.trim().isEmpty()) {
            throw new IllegalArgumentException("O artista é obrigatório");
        }
        if (nota != null && (nota < 1 || nota > 5)) {
            throw new IllegalArgumentException("A nota deve ser entre 1 e 5 estrelas");
        }
    }
}
