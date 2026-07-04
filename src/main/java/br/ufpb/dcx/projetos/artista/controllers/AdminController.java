package br.ufpb.dcx.projetos.artista.controllers;

import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import br.ufpb.dcx.projetos.login.models.Role;
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
        app.get("/admin/verificacoes", ctx -> this.listarPendentes(ctx), Role.ADMIN);
        app.post("/admin/verificacoes/{id}/aprovar", ctx -> this.aprovar(ctx), Role.ADMIN);
        app.post("/admin/verificacoes/{id}/rejeitar", ctx -> this.rejeitar(ctx), Role.ADMIN);
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
