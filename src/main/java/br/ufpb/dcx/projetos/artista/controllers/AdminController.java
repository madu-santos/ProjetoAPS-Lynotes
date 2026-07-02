package br.ufpb.dcx.projetos.artista.controllers;

import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class AdminController {

    private final ArtistaService service;

    public AdminController(ArtistaService service) {
        this.service = service;
    }

    public void registrarRotas(Javalin app) {
        app.get("/admin/verificacoes", this::listarPendentes);
        app.post("/admin/verificacoes/{id}/aprovar", this::aprovar);
        app.post("/admin/verificacoes/{id}/rejeitar", this::rejeitar);
    }

    private void listarPendentes(Context ctx) {
        List<Artista> pendentes = service.listarPendentes();
        ctx.render("admin/verificacoes", Map.of("artistas", pendentes));
    }

    private void aprovar(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            service.aprovar(id);
        } catch (Exception e) {
            // Log erro ou tratar
        }
        ctx.redirect("/admin/verificacoes");
    }

    private void rejeitar(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            service.rejeitar(id);
        } catch (Exception e) {
            // Log erro ou tratar
        }
        ctx.redirect("/admin/verificacoes");
    }
}
