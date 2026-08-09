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
    private static final java.util.Map<String, String> titleCache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.Map<String, Integer> durationCache = new java.util.concurrent.ConcurrentHashMap<>();

    public PlaylistService(PlaylistRepository playlistRepository) {
        this.playlistRepository = playlistRepository;
    }

    public Playlist createPlaylist(String nome, String usuarioId) {
        validarUsuarioId(usuarioId);
        validarNome(nome);
        boolean nomeJaExiste = playlistRepository.findByUsuarioId(usuarioId).stream()
                .anyMatch(p -> p.getNome().equalsIgnoreCase(nome.trim()));
        if (nomeJaExiste) {
            throw new IllegalArgumentException("Você já possui uma playlist com este nome.");
        }
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
        boolean nomeJaExiste = playlistRepository.findByUsuarioId(usuarioId).stream()
                .filter(p -> !p.getId().equals(id))
                .anyMatch(p -> p.getNome().equalsIgnoreCase(nome.trim()));
        if (nomeJaExiste) {
            throw new IllegalArgumentException("Você já possui uma playlist com este nome.");
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

    public void sincronizarMusicasCatalogadas(String usuarioId, List<br.ufpb.dcx.projetos.musica.models.Musica> musicas) {
        if (usuarioId == null || musicas == null || musicas.isEmpty()) return;
        Playlist hidden = findOrCreateHiddenPlaylist(usuarioId);
        List<PlaylistItem> existingItems = playlistRepository.findItemsByPlaylistId(hidden.getId());
        java.util.Set<String> existingVideoIds = new java.util.HashSet<>();
        java.util.Set<String> existingTitles = new java.util.HashSet<>();
        if (existingItems != null) {
            for (PlaylistItem it : existingItems) {
                if (it.getVideoId() != null) existingVideoIds.add(it.getVideoId().toLowerCase());
                if (it.getTitulo() != null) existingTitles.add(it.getTitulo().trim().toLowerCase());
            }
        }

        for (br.ufpb.dcx.projetos.musica.models.Musica m : musicas) {
            if (m.isOcultaDaBiblioteca()) continue;
            String vId = m.getYoutubeId();
            if (vId == null || vId.isBlank()) {
                vId = extractVideoId(m.getYoutubeUrl());
            }
            String url = m.getYoutubeUrl();
            if (url == null || url.isBlank()) {
                url = (vId != null && !vId.startsWith("yt_")) ? "https://www.youtube.com/watch?v=" + vId : "https://www.youtube.com";
            }
            String title = (m.getTitulo() != null && !m.getTitulo().isBlank()) ? m.getTitulo() : "Faixa Sem Título";
            
            boolean jaExiste = (vId != null && !vId.startsWith("yt_") && existingVideoIds.contains(vId.toLowerCase())) ||
                               existingTitles.contains(title.trim().toLowerCase());
            if (!jaExiste) {
                try {
                    int ordem = playlistRepository.countItemsByPlaylistId(hidden.getId()) + 1;
                    PlaylistItem item = new PlaylistItem(
                            java.util.UUID.randomUUID().toString(),
                            hidden.getId(), url, vId, title, ordem, java.time.Instant.now());
                    playlistRepository.createItem(item);
                    if (vId != null) existingVideoIds.add(vId.toLowerCase());
                    existingTitles.add(title.trim().toLowerCase());
                } catch (Exception ignored) {}
            }
        }
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

    public PlaylistItem addItem(String playlistId, String url, String videoId, String usuarioId, String tituloParam) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Playlist não encontrada."));

        validarUrl(url);

        String titulo = (tituloParam != null && !tituloParam.isBlank()) ? tituloParam.trim() : fetchYouTubeTitle(url, videoId);

        if (itemExisteNaPlaylist(playlistId, videoId, url, titulo)) {
            List<PlaylistItem> existentes = playlistRepository.findItemsByPlaylistId(playlistId);
            if (existentes != null && !existentes.isEmpty()) {
                String ext = extractVideoId(url);
                return existentes.stream().filter(item ->
                        (titulo != null && item.getTitulo() != null && item.getTitulo().trim().equalsIgnoreCase(titulo)) ||
                        (videoId != null && !videoId.isBlank() && item.getVideoId().equalsIgnoreCase(videoId)) ||
                        (ext != null && !ext.isBlank() && item.getVideoId().equalsIgnoreCase(ext)) ||
                        (url != null && !url.isBlank() && item.getUrl().trim().equalsIgnoreCase(url.trim()))
                ).findFirst().orElse(existentes.get(0));
            }
        }

        int ordem = playlistRepository.countItemsByPlaylistId(playlistId) + 1;
        PlaylistItem item = new PlaylistItem(
                java.util.UUID.randomUUID().toString(),
                playlistId, url, videoId, titulo, ordem, java.time.Instant.now());
        playlistRepository.createItem(item);
        return item;
    }

    public PlaylistItem addItem(String playlistId, String url, String videoId, String usuarioId) {
        return addItem(playlistId, url, videoId, usuarioId, null);
    }

    public boolean itemExisteNaPlaylist(String playlistId, String videoId, String url, String tituloParam) {
        List<PlaylistItem> items = playlistRepository.findItemsByPlaylistId(playlistId);
        if (items == null || items.isEmpty()) return false;
        String extracted = extractVideoId(url);
        String tituloNorm = (tituloParam != null && !tituloParam.isBlank()) ? tituloParam.trim() : null;
        return items.stream().anyMatch(item ->
                (tituloNorm != null && item.getTitulo() != null && item.getTitulo().trim().equalsIgnoreCase(tituloNorm)) ||
                (videoId != null && !videoId.isBlank() && item.getVideoId().equalsIgnoreCase(videoId)) ||
                (extracted != null && !extracted.isBlank() && item.getVideoId().equalsIgnoreCase(extracted)) ||
                (url != null && !url.isBlank() && item.getUrl().trim().equalsIgnoreCase(url.trim()))
        );
    }

    public boolean itemExisteNaPlaylist(String playlistId, String videoId, String url) {
        return itemExisteNaPlaylist(playlistId, videoId, url, null);
    }

    public void removerMusicaDeTodasPlaylists(String titulo, String artista, String youtubeId, String usuarioId) {
        List<Playlist> playlists = playlistRepository.findByUsuarioId(usuarioId);
        if (playlists == null || playlists.isEmpty()) return;

        for (Playlist pl : playlists) {
            List<PlaylistItem> items = playlistRepository.findItemsByPlaylistId(pl.getId());
            if (items == null) continue;

            for (PlaylistItem item : items) {
                boolean matchVideoId = youtubeId != null && !youtubeId.isBlank() && youtubeId.equalsIgnoreCase(item.getVideoId());
                boolean matchTitulo = titulo != null && item.getTitulo() != null && item.getTitulo().toLowerCase().contains(titulo.toLowerCase().trim());

                if (matchVideoId || matchTitulo) {
                    playlistRepository.deleteItem(item.getId());
                }
            }
        }
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

        if (deleteFromCatalog && musicaService != null) {
            List<br.ufpb.dcx.projetos.musica.models.Musica> userMusicas = musicaService.findByUsuarioId(usuarioId);
            for (br.ufpb.dcx.projetos.musica.models.Musica m : userMusicas) {
                boolean matchVideoId = item.getVideoId() != null && !item.getVideoId().isBlank() && item.getVideoId().equals(m.getYoutubeId());
                boolean matchUrl = item.getUrl() != null && !item.getUrl().isBlank() && item.getUrl().equals(m.getYoutubeUrl());
                boolean matchTitulo = item.getTitulo() != null && !item.getTitulo().isBlank() && item.getTitulo().equalsIgnoreCase(m.getTitulo());
                if (matchVideoId || matchUrl || matchTitulo) {
                    try {
                        musicaService.deleteById(m.getId());
                    } catch (Exception ignored) {}
                }
            }
        }
    }

    public void alternarOcultaItem(String playlistId, String itemId, String usuarioId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .filter(p -> p.getUsuarioId().equals(usuarioId))
                .orElseThrow(() -> new ResourceNotFoundException("Playlist não encontrada."));

        PlaylistItem item = playlistRepository.findItemById(itemId)
                .filter(i -> i.getPlaylistId().equals(playlistId))
                .orElseThrow(() -> new ResourceNotFoundException("Item de playlist não encontrado."));

        playlistRepository.alternarOcultaItem(itemId);
    }

    public Optional<Playlist> findById(String playlistId) {
        return playlistRepository.findById(playlistId);
    }

    public java.util.Map<String, List<PlaylistItem>> findItemsForPlaylists(List<String> playlistIds) {
        List<PlaylistItem> allItems = playlistRepository.findItemsByPlaylistIds(playlistIds);
        java.util.Map<String, List<PlaylistItem>> map = new java.util.HashMap<>();
        for (String plId : playlistIds) {
            map.put(plId, new java.util.ArrayList<>());
        }
        for (PlaylistItem item : allItems) {
            if (map.containsKey(item.getPlaylistId())) {
                map.get(item.getPlaylistId()).add(item);
            }
        }
        return map;
    }

    private String fetchYouTubeTitle(String url, String videoId) {
        String cacheKey = (videoId != null && !videoId.isBlank()) ? videoId : url;
        if (cacheKey != null && titleCache.containsKey(cacheKey)) {
            return titleCache.get(cacheKey);
        }
        String title = executarFetchTitleSemCache(url, videoId);
        if (cacheKey != null && title != null) {
            titleCache.put(cacheKey, title);
        }
        return title;
    }

    private String executarFetchTitleSemCache(String url, String videoId) {
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
        if (durationCache.containsKey(url)) {
            return durationCache.get(url);
        }
        Integer duration = executarFetchDurationSemCache(url);
        if (duration != null) {
            durationCache.put(url, duration);
        }
        return duration;
    }

    private Integer executarFetchDurationSemCache(String url) {
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
