package br.ufpb.dcx.projetos.artista.controllers;

import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

public class AdminController {

    private final ArtistaService service;

    public AdminController(ArtistaService service) {
        this.service = service;
    }

    public void listarPendentes(Context ctx) {
        List<Artista> pendentes = service.listarPendentes();
        ctx.render("admin/verificacoes", Map.of("artistas", pendentes));
    }

    public void aprovar(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            service.aprovar(id);
        } catch (Exception e) {
            // Log erro ou tratar
        }
        ctx.redirect("/admin/verificacoes");
    }

    public void rejeitar(Context ctx) {
        String id = ctx.pathParam("id");
        try {
            service.rejeitar(id);
        } catch (Exception e) {
            // Log erro ou tratar
        }
        ctx.redirect("/admin/verificacoes");
    }
}
