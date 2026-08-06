package br.ufpb.dcx.projetos.infra.youtube.controllers;

import br.ufpb.dcx.projetos.infra.youtube.YouTubeService;
import br.ufpb.dcx.projetos.infra.youtube.dto.YouTubeItemDTO;
import io.javalin.http.Context;

import java.util.List;

public final class YouTubeController {

    private final YouTubeService youtubeService;

    public YouTubeController(YouTubeService youtubeService) {
        this.youtubeService = youtubeService;
    }

    public void buscar(Context ctx) {
        String query = ctx.queryParam("q");
        String tipo = ctx.queryParam("tipo");
        boolean realVideo = "true".equalsIgnoreCase(ctx.queryParam("realVideo"));

        if (query == null || query.isBlank()) {
            ctx.json(List.of());
            return;
        }

        List<YouTubeItemDTO> resultados = youtubeService.buscar(query.trim(), tipo, realVideo);
        ctx.json(resultados);
    }
}
