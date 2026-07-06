package br.ufpb.dcx.projetos.comunidade.controllers;

import br.ufpb.dcx.projetos.comunidade.services.ComunidadeService;
import br.ufpb.dcx.projetos.comunidade.views.PostViewDTO;
import br.ufpb.dcx.projetos.login.models.Role;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

public class ComunidadeController {

    private final ComunidadeService service;

    public ComunidadeController(ComunidadeService service) {
        this.service = service;
    }

    public void registrarRotas(Javalin app) {
        app.get("/comunidade", this::feed, Role.USER);
        app.post("/comunidade/postar", this::postar, Role.USER);
        app.get("/comunidade/post/{id}", this::detalhePost, Role.USER);
        app.post("/comunidade/post/{id}/comentar", this::comentar, Role.USER);
        app.post("/comunidade/post/{id}/estrela", this::estrela, Role.USER);
    }

    private void feed(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String q = ctx.queryParam("q"); // busca de usuario (opcional)
        
        // Aqui o filtro de usuário será implementado no front com busca de usuarios real, mas por enquanto:
        // Apenas listamos o feed
        var posts = service.listarFeed(usuarioLogadoId, null);
        
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("usuarioLogadoId", usuarioLogadoId);
        modelo.put("posts", posts);
        modelo.put("erro", ctx.sessionAttribute("erroComunidade")); // flash message
        ctx.sessionAttribute("erroComunidade", null); // limpa a msg
        
        ctx.render("comunidade/feed", modelo);
    }

    private void postar(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String conteudo = ctx.formParam("conteudo");
        
        try {
            service.criarPost(usuarioLogadoId, conteudo);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        
        String urlReferencia = ctx.header("Referer");
        if (urlReferencia != null) {
            ctx.redirect(urlReferencia);
        } else {
            ctx.redirect("/comunidade");
        }
    }

    private void detalhePost(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String postId = ctx.pathParam("id");
        
        var postOpt = service.buscarPostView(postId, usuarioLogadoId);
        if (postOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Post não encontrado");
            return;
        }
        
        var comentarios = service.listarComentarios(postId);
        
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("usuarioLogadoId", usuarioLogadoId);
        modelo.put("post", postOpt.get());
        modelo.put("comentarios", comentarios);
        modelo.put("erro", ctx.sessionAttribute("erroComunidade")); 
        ctx.sessionAttribute("erroComunidade", null); 
        
        ctx.render("comunidade/post", modelo);
    }

    private void comentar(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String postId = ctx.pathParam("id");
        String conteudo = ctx.formParam("conteudo");
        
        try {
            service.criarComentario(postId, usuarioLogadoId, conteudo);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        
        ctx.redirect("/comunidade/post/" + postId);
    }

    private void estrela(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String postId = ctx.pathParam("id");
        
        try {
            service.alternarEstrela(postId, usuarioLogadoId);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        
        // Redireciona de volta para onde estava (feed ou post detalhe)
        String urlReferencia = ctx.header("Referer");
        if (urlReferencia != null) {
            ctx.redirect(urlReferencia);
        } else {
            ctx.redirect("/comunidade");
        }
    }
}
