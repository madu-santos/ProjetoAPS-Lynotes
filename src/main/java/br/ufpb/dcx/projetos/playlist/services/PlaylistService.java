package br.ufpb.dcx.projetos.playlist.services;

import br.ufpb.dcx.projetos.exceptions.ResourceNotFoundException;
import br.ufpb.dcx.projetos.playlist.models.Playlist;
import br.ufpb.dcx.projetos.playlist.models.PlaylistItem;
import br.ufpb.dcx.projetos.playlist.models.PlaylistWithItems;
import br.ufpb.dcx.projetos.playlist.repositories.PlaylistRepository;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Optional;

public class PlaylistService {

    private final PlaylistRepository playlistRepository;

    public PlaylistService(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    public Playlist createPlaylist(String nome, String usuarioId) {
        validarUsuarioId(usuarioId);
        validarNome(nome);
        Playlist playlist = Playlist.novo(nome, usuarioId);
        playlistRepository.create(playlist);
        return playlist;
    }

    public List<Playlist> findByUsuarioId(String usuarioId) {
        validarUsuarioId(usuarioId);
        return playlistRepository.findByUsuarioId(usuarioId).stream()
                .filter(playlist -> !playlist.isOculta() || "Músicas Catalogadas".equalsIgnoreCase(playlist.getNome()) || "Músicas do seu catálogo".equalsIgnoreCase(playlist.getNome()))
                .toList();
    }

    public List<Playlist> findAllByUsuarioId(String usuarioId) {
        validarUsuarioId(usuarioId);
        return playlistRepository.findByUsuarioId(usuarioId);
    }

    public List<Playlist> search(String termo, String usuarioId) {
        validarUsuarioId(usuarioId);
        return playlistRepository.buscar(termo, usuarioId);
    }

    public void updatePlaylist(String id, String nome, boolean oculta, String usuarioId) {
        validarNome(nome);
        validarUsuarioId(usuarioId);
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Playlist não encontrada."));
        if (!playlist.getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("Você não tem permissão para editar esta playlist.");
        }
        Playlist playlistAtualizada = new Playlist(playlist.getId(), nome.trim(), playlist.getUsuarioId(), oculta, playlist.getCriadoEm());
        playlistRepository.update(playlistAtualizada);
    }

    public void deletePlaylist(String id, String usuarioId) {
        validarUsuarioId(usuarioId);
        Playlist playlist = playlistRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Playlist não encontrada."));
        if (!playlist.getUsuarioId().equals(usuarioId)) {
            throw new IllegalArgumentException("Você não tem permissão para excluir esta playlist.");
        }
        if (playlist.isOculta() && ("Músicas Catalogadas".equalsIgnoreCase(playlist.getNome()) || "Músicas do seu catálogo".equalsIgnoreCase(playlist.getNome()))) {
            throw new IllegalArgumentException("Você não pode excluir a playlist oculta de Músicas Catalogadas.");
        }
        playlistRepository.delete(id);
    }

    public Playlist findOrCreateHiddenPlaylist(String usuarioId) {
        validarUsuarioId(usuarioId);
        return playlistRepository.findByUsuarioId(usuarioId).stream()
                .filter(playlist -> playlist.isOculta() || "Músicas Catalogadas".equalsIgnoreCase(playlist.getNome()) || "Músicas do seu catálogo".equalsIgnoreCase(playlist.getNome()))
                .findFirst()
                .orElseGet(() -> {
                    Playlist playlist = Playlist.novo("Músicas Catalogadas", usuarioId, true);
                    playlistRepository.create(playlist);
                    return playlist;
                });
    }

    public String extractVideoId(String url) {
        if (url == null || url.isBlank()) {
            return "yt_default";
        }
        String query = url.trim();
        if (query.contains("v=")) {
            int begin = query.indexOf("v=") + 2;
            int end = query.indexOf('&', begin);
            String id = end > 0 ? query.substring(begin, end) : query.substring(begin);
            if (!id.isBlank()) return id;
        }
        if (query.contains("youtu.be/")) {
            int begin = query.indexOf("youtu.be/") + 9;
            int end = query.indexOf('?', begin);
            String id = end > 0 ? query.substring(begin, end) : query.substring(begin);
            if (!id.isBlank()) return id;
        }
        return "yt_" + Math.abs(query.hashCode());
    }

    public PlaylistWithItems findPlaylistWithItems(String playlistId, String usuarioId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Playlist não encontrada."));
        return new PlaylistWithItems(playlist, playlistRepository.findItemsByPlaylistId(playlistId));
    }

    public PlaylistItem addItem(String playlistId, String url, String videoId, String usuarioId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Playlist não encontrada."));

        validarUrl(url);

        // Prevent duplicate items in the same playlist
        Optional<PlaylistItem> existingItem = playlistRepository.findItemsByPlaylistId(playlistId).stream()
                .filter(item -> item.getVideoId().equals(videoId))
                .findFirst();
        if (existingItem.isPresent()) {
            throw new IllegalArgumentException("Essa música já está salva nessa playlist.");
        }

        int ordem = playlistRepository.countItemsByPlaylistId(playlistId) + 1;
        String titulo = fetchYouTubeTitle(url, videoId);
        PlaylistItem item = new PlaylistItem(
                java.util.UUID.randomUUID().toString(),
                playlistId, url, videoId, titulo, ordem, java.time.Instant.now());
        playlistRepository.createItem(item);
        return item;
    }

    public String getPlaylistCapaUrl(String playlistId) {
        List<PlaylistItem> items = playlistRepository.findItemsByPlaylistId(playlistId);
        if (items != null && !items.isEmpty()) {
            return "https://img.youtube.com/vi/" + items.get(0).getVideoId() + "/hqdefault.jpg";
        }
        return null;
    }

    public List<String> getPlaylistFirstFourCovers(String playlistId) {
        List<PlaylistItem> items = playlistRepository.findItemsByPlaylistId(playlistId);
        if (items != null) {
            return items.stream()
                    .limit(4)
                    .map(item -> "https://img.youtube.com/vi/" + item.getVideoId() + "/hqdefault.jpg")
                    .toList();
        }
        return List.of();
    }

    public void removeItem(String playlistId, String itemId, String usuarioId) {
        removeItem(playlistId, itemId, usuarioId, false, null);
    }

    public void removeItem(String playlistId, String itemId, String usuarioId, boolean deleteFromCatalog, br.ufpb.dcx.projetos.musica.services.MusicaService musicaService) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Playlist não encontrada."));

        PlaylistItem item = playlistRepository.findItemById(itemId)
                .filter(i -> i.getPlaylistId().equals(playlistId))
                .orElseThrow(() -> new ResourceNotFoundException("Item de playlist não encontrado."));

        if (!playlistRepository.deleteItem(itemId)) {
            throw new ResourceNotFoundException("Falha ao remover item da playlist.");
        }

        if (deleteFromCatalog && musicaService != null && item.getVideoId() != null) {
            musicaService.findByUsuarioId(usuarioId).stream()
                    .filter(m -> item.getVideoId().equals(m.getYoutubeId()))
                    .findFirst()
                    .ifPresent(m -> musicaService.deleteById(m.getId()));
        }
    }

    public Optional<Playlist> findById(String playlistId) {
        return playlistRepository.findById(playlistId);
    }

    private String fetchYouTubeTitle(String url, String videoId) {
        if (url != null && url.contains("search_query=")) {
            try {
                int start = url.indexOf("search_query=") + 13;
                int end = url.indexOf('&', start);
                String query = end > 0 ? url.substring(start, end) : url.substring(start);
                String decoded = java.net.URLDecoder.decode(query, java.nio.charset.StandardCharsets.UTF_8);
                if (!decoded.isBlank()) {
                    return decoded;
                }
            } catch (Exception e) {
                // fallback
            }
        }
        try {
            String oEmbedUrl = "https://www.youtube.com/oembed?url="
                    + java.net.URLEncoder.encode(url, java.nio.charset.StandardCharsets.UTF_8)
                    + "&format=json";
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(oEmbedUrl))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                // Extrai "title" do JSON sem dependência extra
                int start = body.indexOf('"', body.indexOf("\"title\":") + 8) + 1;
                int end   = body.indexOf('"', start);
                if (start > 0 && end > start) {
                    return body.substring(start, end);
                }
            }
        } catch (IOException | InterruptedException e) {
            // fallback silencioso
        }
        return "YouTube - " + videoId;
    }

    public Integer fetchYouTubeDuration(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(java.time.Duration.ofSeconds(5))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "Mozilla/5.0")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                String body = response.body();
                java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"lengthSeconds\":\"(\\d+)\"");
                java.util.regex.Matcher m = p.matcher(body);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
        } catch (Exception e) {
            // fallback
        }
        return null;
    }

    private void validarNome(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException("O nome da playlist é obrigatório.");
        }
    }

    private void validarUsuarioId(String usuarioId) {
        if (usuarioId == null || usuarioId.isBlank()) {
            throw new IllegalArgumentException("Usuário não autenticado.");
        }
    }

    private void validarUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("A URL do YouTube é obrigatória.");
        }
        if (!url.toLowerCase().contains("youtube.com") && !url.toLowerCase().contains("youtu.be") && !url.toLowerCase().contains("http")) {
            throw new IllegalArgumentException("A URL precisa ser um link do YouTube válido.");
        }
    }
}
