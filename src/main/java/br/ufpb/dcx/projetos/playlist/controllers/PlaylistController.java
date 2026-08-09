package br.ufpb.dcx.projetos.playlist.controllers;

import br.ufpb.dcx.projetos.playlist.models.Playlist;
import br.ufpb.dcx.projetos.playlist.models.PlaylistItem;
import br.ufpb.dcx.projetos.playlist.models.PlaylistWithItems;
import br.ufpb.dcx.projetos.playlist.services.PlaylistService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public final class PlaylistController {

    private final PlaylistService service;
    private final br.ufpb.dcx.projetos.musica.services.MusicaService musicaService;
    private final br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService;

    public PlaylistController(PlaylistService service) {
        this.service = service;
        this.musicaService = null;
        this.comunidadeService = null;
    }

    public PlaylistController(PlaylistService service, br.ufpb.dcx.projetos.musica.services.MusicaService musicaService) {
        this.service = service;
        this.musicaService = musicaService;
        this.comunidadeService = null;
    }

    public PlaylistController(PlaylistService service, br.ufpb.dcx.projetos.musica.services.MusicaService musicaService, br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService) {
        this.service = service;
        this.musicaService = musicaService;
        this.comunidadeService = comunidadeService;
    }

    public void listar(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        if (musicaService != null) {
            service.sincronizarMusicasCatalogadas(usuarioId, musicaService.findByUsuarioId(usuarioId));
        }
        String busca = ctx.queryParam("busca");
        java.util.List<Playlist> playlists = service.search(busca, usuarioId);
        
        java.util.Map<String, String> musicCovers = new java.util.HashMap<>();
        if (musicaService != null) {
            for (br.ufpb.dcx.projetos.musica.models.Musica m : musicaService.findByUsuarioId(usuarioId)) {
                if (m.getYoutubeId() != null && m.getCapaUrl() != null && !m.getCapaUrl().isBlank()) {
                    musicCovers.put(m.getYoutubeId(), m.getCapaUrl());
                }
            }
        }

        java.util.List<String> plIds = playlists.stream().map(Playlist::getId).toList();
        java.util.Map<String, java.util.List<PlaylistItem>> allPlaylistItems = service.findItemsForPlaylists(plIds);

        java.util.Map<String, java.util.List<String>> playlistCovers = new java.util.HashMap<>();
        for (Playlist pl : playlists) {
            java.util.List<PlaylistItem> items = allPlaylistItems.getOrDefault(pl.getId(), java.util.List.of());
            java.util.List<String> covers = items.stream()
                    .limit(4)
                    .map(item -> {
                        if (musicCovers.containsKey(item.getVideoId())) {
                            return musicCovers.get(item.getVideoId());
                        }
                        return "https://img.youtube.com/vi/" + item.getVideoId() + "/hqdefault.jpg";
                    })
                    .toList();
            playlistCovers.put(pl.getId(), covers);
        }

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("playlists", playlists);
        modelo.put("playlistCovers", playlistCovers);
        modelo.put("busca", busca != null ? busca : "");
        modelo.put("buscaAtiva", busca != null && !busca.trim().isEmpty());
        ctx.render("playlists/lista", modelo);
    }

    public void exibirFormularioCriacao(Context ctx) {
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("edicao", false);
        modelo.put("acao", "/playlists");
        modelo.put("nome", "");
        ctx.render("playlists/formulario", modelo);
    }

    public void exibirFormularioEdicao(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        PlaylistWithItems playlist = service.findPlaylistWithItems(playlistId, usuarioId);

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("edicao", true);
        modelo.put("acao", "/playlists/" + playlistId + "/editar");
        modelo.put("playlist", playlist.getPlaylist());
        modelo.put("nome", playlist.getPlaylist().getNome());
        ctx.render("playlists/formulario", modelo);
    }

    public void criarPlaylist(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String nome = ctx.formParam("nome");

        try {
            Playlist playlist = service.createPlaylist(nome, usuarioId);
            String compartilhar = ctx.formParam("compartilharNaComunidade");
            if (comunidadeService != null && ("on".equals(compartilhar) || "true".equals(compartilhar))) {
                String msg = "Criou a playlist: **" + playlist.getNome() + "**";
                comunidadeService.criarPostCompartilhado(usuarioId, "SHARE_PLAYLIST", playlist.getId(), msg);
            }
            ctx.redirect("/playlists/" + playlist.getId(), HttpStatus.SEE_OTHER);
        } catch (IllegalArgumentException e) {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("erro", e.getMessage());
            modelo.put("edicao", false);
            modelo.put("acao", "/playlists");
            modelo.put("nome", nome != null ? nome : "");
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).render("playlists/formulario", modelo);
        }
    }

    private java.util.List<PlaylistItem> sanitizePlaylistItems(java.util.List<PlaylistItem> items) {
        java.util.List<PlaylistItem> sanitized = new java.util.ArrayList<>();
        for (PlaylistItem item : items) {
            String title = item.getTitulo();
            if (title == null || title.isBlank()) {
                title = "Música Sem Título";
            } else if (title.startsWith("YouTube - ")) {
                title = title.replace("YouTube - ", "");
            }
            sanitized.add(new PlaylistItem(item.getId(), item.getPlaylistId(), item.getUrl(), item.getVideoId(), title, item.getOrdem(), item.getCriadoEm()));
        }
        return sanitized;
    }

    private java.util.List<br.ufpb.dcx.projetos.playlist.views.PlaylistItemView> mapToViews(java.util.List<PlaylistItem> items, String usuarioId) {
        java.util.List<br.ufpb.dcx.projetos.musica.models.Musica> userMusicas = musicaService != null ? musicaService.findByUsuarioId(usuarioId) : java.util.List.of();
        java.util.Map<String, br.ufpb.dcx.projetos.musica.models.Musica> musicMap = new java.util.HashMap<>();
        for (var m : userMusicas) {
            if (m.getYoutubeId() != null) {
                musicMap.put(m.getYoutubeId(), m);
            }
        }

        java.util.List<br.ufpb.dcx.projetos.playlist.views.PlaylistItemView> views = new java.util.ArrayList<>();
        for (PlaylistItem item : items) {
            String title = item.getTitulo();
            if (title == null || title.isBlank()) {
                title = "Música Sem Título";
            } else if (title.startsWith("YouTube - ")) {
                title = title.replace("YouTube - ", "");
            }

            String artist = "";
            String capa = "https://img.youtube.com/vi/" + item.getVideoId() + "/hqdefault.jpg";

            br.ufpb.dcx.projetos.musica.models.Musica matching = musicMap.get(item.getVideoId());
            if (matching != null) {
                title = matching.getTitulo();
                artist = matching.getArtista();
                if (matching.getCapaUrl() != null && !matching.getCapaUrl().isBlank()) {
                    capa = matching.getCapaUrl();
                }
            } else {
                if (title.contains(" - ")) {
                    String[] parts = title.split(" - ", 2);
                    artist = parts[0].trim();
                    title = parts[1].trim();
                } else if (title.contains("-")) {
                    String[] parts = title.split("-", 2);
                    artist = parts[0].trim();
                    title = parts[1].trim();
                } else {
                    artist = "Artista do Vídeo";
                }
            }

            views.add(new br.ufpb.dcx.projetos.playlist.views.PlaylistItemView(
                    item.getId(),
                    item.getVideoId(),
                    item.getUrl(),
                    title,
                    artist,
                    capa,
                    item.isOculta()
            ));
        }
        return views;
    }

    public void exibirPlaylist(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        if (musicaService != null) {
            service.sincronizarMusicasCatalogadas(usuarioId, musicaService.findByUsuarioId(usuarioId));
        }
        PlaylistWithItems playlist = service.findPlaylistWithItems(playlistId, usuarioId);

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("playlist", playlist.getPlaylist());
        modelo.put("items", mapToViews(playlist.getItems(), usuarioId));
        ctx.render("playlists/detalhe", modelo);
    }

    public void adicionarItem(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");

        String titulo = ctx.formParam("titulo");
        String artista = ctx.formParam("artista");
        String duracaoStr = ctx.formParam("duracaoSegundos");
        String youtubeUrl = ctx.formParam("youtubeUrl");
        if (youtubeUrl == null || youtubeUrl.isBlank()) {
            youtubeUrl = ctx.formParam("url");
        }
        String youtubeId = ctx.formParam("youtubeId");
        String capaUrl = ctx.formParam("capaUrl");

        try {
            if ((youtubeId == null || youtubeId.isBlank()) && youtubeUrl != null && !youtubeUrl.isBlank()) {
                try {
                    youtubeId = service.extractVideoId(youtubeUrl);
                } catch (Exception ignored) {}
            }

            if (youtubeUrl == null || youtubeUrl.isBlank()) {
                if (youtubeId != null && !youtubeId.isBlank()) {
                    youtubeUrl = "https://www.youtube.com/watch?v=" + youtubeId;
                }
            }

            if (youtubeUrl == null || youtubeUrl.isBlank()) {
                throw new IllegalArgumentException("Informações da música incompletas.");
            }

            if (youtubeId == null || youtubeId.isBlank()) {
                try {
                    youtubeId = service.extractVideoId(youtubeUrl);
                } catch (Exception ignored) {}
            }

            if (musicaService != null) {
                final String finalYtId = youtubeId;
                final String finalTitulo = titulo;
                final String finalArtista = artista;

                var musicaExistente = musicaService.findByUsuarioId(usuarioId).stream()
                        .filter(m -> (finalYtId != null && finalYtId.equals(m.getYoutubeId())) ||
                                (finalTitulo != null && finalArtista != null &&
                                 m.getTitulo() != null && m.getArtista() != null &&
                                 m.getTitulo().trim().equalsIgnoreCase(finalTitulo.trim()) &&
                                 m.getArtista().trim().equalsIgnoreCase(finalArtista.trim())))
                        .findFirst();

                if (musicaExistente.isPresent()) {
                    var m = musicaExistente.get();
                    if (titulo == null || titulo.isBlank()) titulo = m.getTitulo();
                    if (artista == null || artista.isBlank()) artista = m.getArtista();
                    if (duracaoStr == null || duracaoStr.isBlank()) {
                        duracaoStr = m.getDuracaoSegundos() != null ? m.getDuracaoSegundos().toString() : null;
                    }
                    if (capaUrl == null || capaUrl.isBlank()) capaUrl = m.getCapaUrl();
                } else if (titulo != null && !titulo.isBlank()) {
                    String musicaId = java.util.UUID.randomUUID().toString();
                    Integer duracao = null;
                    if (duracaoStr != null && !duracaoStr.isBlank()) {
                        try {
                            duracao = Integer.parseInt(duracaoStr);
                        } catch (Exception ignored) {}
                    }
                    br.ufpb.dcx.projetos.musica.models.Musica novaMusica = new br.ufpb.dcx.projetos.musica.models.Musica(
                        musicaId,
                        titulo,
                        artista,
                        "Outros", // Default genre
                        duracao,
                        null, // Resenha
                        null, // Nota
                        null, // SpotifyUrl
                        youtubeUrl,
                        null, // AlbumId
                        usuarioId,
                        youtubeId,
                        capaUrl,
                        false
                    );
                    musicaService.save(novaMusica);

                    // Add to default playlist "Músicas Catalogadas"
                    String defaultPlaylistId = service.findOrCreateHiddenPlaylist(usuarioId).getId();
                    try {
                        service.addItem(defaultPlaylistId, youtubeUrl, youtubeId, usuarioId, titulo);
                    } catch (Exception ignored) {}
                }
            }

            // Add to target playlist (se não for a mesma playlist oculta já inserida)
            String defaultId = service.findOrCreateHiddenPlaylist(usuarioId).getId();
            if (!defaultId.equals(playlistId)) {
                service.addItem(playlistId, youtubeUrl, youtubeId, usuarioId, titulo);
            }

            ctx.redirect("/playlists/" + playlistId, HttpStatus.SEE_OTHER);
        } catch (IllegalArgumentException e) {
            Map<String, Object> modelo = new HashMap<>();
            PlaylistWithItems playlist = service.findPlaylistWithItems(playlistId, usuarioId);
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("playlist", playlist.getPlaylist());
            modelo.put("items", sanitizePlaylistItems(playlist.getItems()));
            modelo.put("erro", e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).render("playlists/detalhe", modelo);
        }
    }

    public void removerItem(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        String itemId = ctx.pathParam("itemId");
        boolean apagarMusica = "true".equalsIgnoreCase(ctx.queryParam("apagarMusica"));

        service.removeItem(playlistId, itemId, usuarioId, apagarMusica, musicaService);
        ctx.redirect("/playlists/" + playlistId, HttpStatus.SEE_OTHER);
    }

    public void alternarOcultaItem(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        String itemId = ctx.pathParam("itemId");

        service.alternarOcultaItem(playlistId, itemId, usuarioId);
        ctx.redirect("/playlists/" + playlistId, HttpStatus.SEE_OTHER);
    }

    public void listarJson(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        if (usuarioId == null || usuarioId.isBlank()) {
            ctx.json(java.util.List.of());
            return;
        }
        ctx.json(service.findAllByUsuarioId(usuarioId));
    }

    public void criarPlaylistJson(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        PlaylistRequest request = ctx.bodyAsClass(PlaylistRequest.class);

        try {
            Playlist playlist = service.createPlaylist(request.nome, usuarioId);
            ctx.status(HttpStatus.CREATED).json(playlist);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).result(e.getMessage());
        }
    }

    public void editarPlaylist(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        String nome = ctx.formParam("nome");
        boolean oculta = ctx.formParam("oculta") != null;

        try {
            service.updatePlaylist(playlistId, nome, oculta, usuarioId);
            ctx.redirect("/playlists/" + playlistId, HttpStatus.SEE_OTHER);
        } catch (IllegalArgumentException e) {
            PlaylistWithItems playlist = service.findPlaylistWithItems(playlistId, usuarioId);
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("edicao", true);
            modelo.put("acao", "/playlists/" + playlistId + "/editar");
            modelo.put("playlist", playlist.getPlaylist());
            modelo.put("nome", nome != null ? nome : playlist.getPlaylist().getNome());
            modelo.put("erro", e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).render("playlists/formulario", modelo);
        }
    }

    public void excluirPlaylist(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");

        try {
            service.deletePlaylist(playlistId, usuarioId);
            String referer = ctx.header("Referer");
            if (referer != null && !referer.contains("/playlists/" + playlistId) && (referer.contains("/musicas") || referer.contains("/home") || referer.endsWith("/playlists") || referer.endsWith("/playlists/"))) {
                ctx.redirect(referer, HttpStatus.SEE_OTHER);
            } else {
                ctx.redirect("/playlists", HttpStatus.SEE_OTHER);
            }
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroPlaylist", e.getMessage());
            ctx.redirect("/playlists/" + playlistId, HttpStatus.SEE_OTHER);
        }
    }

    // JSON API Endpoints
    public void obterPlaylistJson(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        if (musicaService != null) {
            service.sincronizarMusicasCatalogadas(usuarioId, musicaService.findByUsuarioId(usuarioId));
        }
        PlaylistWithItems playlist = service.findPlaylistWithItems(playlistId, usuarioId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("playlist", playlist.getPlaylist());
        payload.put("items", mapToViews(playlist.getItems(), usuarioId));
        ctx.json(payload);
    }

    public void checkDuplicateJson(Context ctx) {
        String playlistId = ctx.pathParam("id");
        String url = ctx.queryParam("url");
        String videoId = ctx.queryParam("videoId");
        boolean duplicada = service.itemExisteNaPlaylist(playlistId, videoId, url);
        ctx.json(Map.of("duplicada", duplicada));
    }

    public void adicionarItemJson(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        PlaylistItemRequest request = ctx.bodyAsClass(PlaylistItemRequest.class);

        try {
            String videoId = service.extractVideoId(request.url);
            PlaylistItem item = service.addItem(playlistId, request.url, videoId, usuarioId);
            ctx.status(HttpStatus.CREATED).json(item);
        } catch (IllegalArgumentException e) {
            responderErroJson(ctx, e.getMessage());
        }
    }

    private void responderErroJson(Context ctx, String mensagem) {
        ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).json(Map.of("erro", mensagem));
    }

    public static final class PlaylistRequest {
        public String nome;

        public PlaylistRequest() {
        }
    }

    public static final class PlaylistItemRequest {
        public String url;

        public PlaylistItemRequest() {
        }
    }
}
