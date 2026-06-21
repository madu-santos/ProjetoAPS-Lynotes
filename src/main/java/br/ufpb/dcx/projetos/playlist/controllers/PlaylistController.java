package br.ufpb.dcx.projetos.playlist.controllers;

import br.ufpb.dcx.projetos.playlist.models.Playlist;
import br.ufpb.dcx.projetos.playlist.models.PlaylistItem;
import br.ufpb.dcx.projetos.playlist.models.PlaylistWithItems;
import br.ufpb.dcx.projetos.playlist.services.PlaylistService;
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
        app.get("/playlists", this::listar);
        app.post("/playlists", this::criarPlaylist);
        app.get("/playlists/{id}", this::exibirPlaylist);
        app.post("/playlists/{id}/items", this::adicionarItem);
        app.post("/playlists/{id}/items/{itemId}/remover", this::removerItem);
        app.get("/api/playlists", this::listarJson);
        app.get("/api/playlists/{id}", this::obterPlaylistJson);
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
            ctx.status(HttpStatus.SEE_OTHER).header("Location", "/playlists");
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
            String videoId = extrairVideoId(url);
            service.addItem(playlistId, url, videoId, usuarioId);
            ctx.status(HttpStatus.SEE_OTHER).header("Location", "/playlists/" + playlistId);
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
        ctx.status(HttpStatus.SEE_OTHER).header("Location", "/playlists/" + playlistId);
    }

    private void listarJson(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        ctx.json(service.findByUsuarioId(usuarioId));
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

    private String extrairVideoId(String url) {
        if (url == null) {
            throw new IllegalArgumentException("A URL do YouTube é obrigatória.");
        }
        String query = url.trim();
        if (query.contains("youtube.com/watch?v=")) {
            int begin = query.indexOf("v=") + 2;
            int end = query.indexOf('&', begin);
            return end > 0 ? query.substring(begin, end) : query.substring(begin);
        }
        if (query.contains("youtu.be/")) {
            int begin = query.indexOf("youtu.be/") + 9;
            int end = query.indexOf('?', begin);
            return end > 0 ? query.substring(begin, end) : query.substring(begin);
        }
        throw new IllegalArgumentException("A URL precisa ser um link do YouTube válido.");
    }
}
