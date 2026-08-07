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

    public PlaylistController(PlaylistService service) {
        this.service = service;
        this.musicaService = null;
    }

    public PlaylistController(PlaylistService service, br.ufpb.dcx.projetos.musica.services.MusicaService musicaService) {
        this.service = service;
        this.musicaService = musicaService;
    }

    public void listar(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
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

        java.util.Map<String, java.util.List<String>> playlistCovers = new java.util.HashMap<>();
        for (Playlist pl : playlists) {
            java.util.List<String> covers = service.findPlaylistWithItems(pl.getId(), usuarioId).getItems().stream()
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
            service.createPlaylist(nome, usuarioId);
            ctx.redirect("/playlists", HttpStatus.SEE_OTHER);
        } catch (IllegalArgumentException e) {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("erro", e.getMessage());
            modelo.put("playlists", service.findAllByUsuarioId(usuarioId));
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).render("playlists/lista", modelo);
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

    public void exibirPlaylist(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        PlaylistWithItems playlist = service.findPlaylistWithItems(playlistId, usuarioId);

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("playlist", playlist.getPlaylist());
        modelo.put("items", sanitizePlaylistItems(playlist.getItems()));
        ctx.render("playlists/detalhe", modelo);
    }

    public void adicionarItem(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");

        String titulo = ctx.formParam("titulo");
        String artista = ctx.formParam("artista");
        String duracaoStr = ctx.formParam("duracaoSegundos");
        String youtubeUrl = ctx.formParam("youtubeUrl");
        String youtubeId = ctx.formParam("youtubeId");
        String capaUrl = ctx.formParam("capaUrl");

        try {
            if (youtubeId != null && !youtubeId.isBlank() && musicaService != null) {
                // Deduplicate: check if already cataloged
                boolean jaExiste = musicaService.findByUsuarioId(usuarioId).stream()
                        .anyMatch(m -> youtubeId.equals(m.getYoutubeId()));
                if (!jaExiste) {
                    String musicaId = java.util.UUID.randomUUID().toString();
                    Integer duracao = null;
                    if (duracaoStr != null && !duracaoStr.isBlank()) {
                        try {
                            duracao = Integer.parseInt(duracaoStr);
                        } catch (Exception e) {}
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
                }

                // Add to default playlist "Músicas Catalogadas"
                String defaultPlaylistId = service.findOrCreateHiddenPlaylist(usuarioId).getId();
                try {
                    service.addItem(defaultPlaylistId, youtubeUrl, youtubeId, usuarioId);
                } catch (Exception ignored) {}

                // Add to the current playlist (unless it is already the default playlist)
                if (!playlistId.equals(defaultPlaylistId)) {
                    service.addItem(playlistId, youtubeUrl, youtubeId, usuarioId);
                }
            } else {
                throw new IllegalArgumentException("Informações da música incompletas.");
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
            ctx.sessionAttribute("erroPlaylist", e.getMessage());
            ctx.redirect("/playlists/" + playlistId, HttpStatus.SEE_OTHER);
        }
    }

    public void excluirPlaylist(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");

        try {
            service.deletePlaylist(playlistId, usuarioId);
            ctx.redirect("/playlists", HttpStatus.SEE_OTHER);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroPlaylist", e.getMessage());
            ctx.redirect("/playlists/" + playlistId, HttpStatus.SEE_OTHER);
        }
    }

    // JSON API Endpoints
    public void obterPlaylistJson(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        PlaylistWithItems playlist = service.findPlaylistWithItems(playlistId, usuarioId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("playlist", playlist.getPlaylist());
        payload.put("items", playlist.getItems());
        ctx.json(payload);
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
