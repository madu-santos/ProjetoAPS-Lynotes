package br.ufpb.dcx.projetos.playlist.controllers;

import br.ufpb.dcx.projetos.playlist.models.Playlist;
import br.ufpb.dcx.projetos.playlist.models.PlaylistItem;
import br.ufpb.dcx.projetos.playlist.models.PlaylistWithItems;
import br.ufpb.dcx.projetos.playlist.services.PlaylistService;
import br.ufpb.dcx.projetos.login.models.Role;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public final class PlaylistController {

    private final PlaylistService service;

    public PlaylistController(PlaylistService service) {
        this.service = service;
    }

    public void registrarRotas(Javalin app) {
        app.get("/playlists", this::listar, Role.USER);
        app.post("/playlists", this::criarPlaylist, Role.USER);
        app.get("/playlists/{id}", this::exibirPlaylist, Role.USER);
        app.post("/playlists/{id}/items", this::adicionarItem, Role.USER);
        app.post("/playlists/{id}/items/{itemId}/remover", this::removerItem, Role.USER);
        app.get("/api/playlists", this::listarJson, Role.USER);
        app.post("/api/playlists", this::criarPlaylistJson, Role.USER);
        app.get("/api/playlists/{id}", this::obterPlaylistJson, Role.USER);
        app.post("/api/playlists/{id}/items", this::adicionarItemJson, Role.USER);
    }

    private void listar(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("playlists", service.findByUsuarioId(usuarioId));
        ctx.render("playlists/lista", modelo);
    }

    private void criarPlaylist(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String nome = ctx.formParam("nome");

        try {
            service.createPlaylist(nome, usuarioId);
            ctx.redirect("/playlists", HttpStatus.SEE_OTHER);
        } catch (IllegalArgumentException e) {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("erro", e.getMessage());
            modelo.put("playlists", service.findByUsuarioId(usuarioId));
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).render("playlists/lista", modelo);
        }
    }

    private void exibirPlaylist(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        PlaylistWithItems playlist = service.findPlaylistWithItems(playlistId, usuarioId);

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("playlist", playlist.getPlaylist());
        modelo.put("items", playlist.getItems());
        ctx.render("playlists/detalhe", modelo);
    }

    private void adicionarItem(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        String url = ctx.formParam("url");

        try {
            String videoId = service.extractVideoId(url);
            service.addItem(playlistId, url, videoId, usuarioId);
            ctx.redirect("/playlists/" + playlistId, HttpStatus.SEE_OTHER);
        } catch (IllegalArgumentException e) {
            Map<String, Object> modelo = new HashMap<>();
            PlaylistWithItems playlist = service.findPlaylistWithItems(playlistId, usuarioId);
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("playlist", playlist.getPlaylist());
            modelo.put("items", playlist.getItems());
            modelo.put("erro", e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).render("playlists/detalhe", modelo);
        }
    }

    private void removerItem(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        String itemId = ctx.pathParam("itemId");

        service.removeItem(playlistId, itemId, usuarioId);
        ctx.redirect("/playlists/" + playlistId, HttpStatus.SEE_OTHER);
    }

    private void listarJson(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        if (usuarioId == null || usuarioId.isBlank()) {
            ctx.json(java.util.List.of());
            return;
        }
        ctx.json(service.findByUsuarioId(usuarioId));
    }

    private void criarPlaylistJson(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        PlaylistRequest request = ctx.bodyAsClass(PlaylistRequest.class);

        try {
            Playlist playlist = service.createPlaylist(request.nome, usuarioId);
            ctx.status(HttpStatus.CREATED).json(playlist);
        } catch (IllegalArgumentException e) {
            responderErroJson(ctx, e.getMessage());
        }
    }

    private void obterPlaylistJson(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String playlistId = ctx.pathParam("id");
        PlaylistWithItems playlist = service.findPlaylistWithItems(playlistId, usuarioId);
        Map<String, Object> payload = new HashMap<>();
        payload.put("playlist", playlist.getPlaylist());
        payload.put("items", playlist.getItems());
        ctx.json(payload);
    }

    private void adicionarItemJson(Context ctx) {
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
