package br.ufpb.dcx.projetos.comunidade.controllers;

import br.ufpb.dcx.projetos.comunidade.services.ComunidadeService;
import br.ufpb.dcx.projetos.comunidade.views.PostViewDTO;
import br.ufpb.dcx.projetos.login.models.Role;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ComunidadeController {

    private final ComunidadeService service;
    private final br.ufpb.dcx.projetos.login.services.UsuarioService usuarioService;
    private final br.ufpb.dcx.projetos.musica.services.MusicaService musicaService;
    private final br.ufpb.dcx.projetos.album.services.AlbumService albumService;
    private final br.ufpb.dcx.projetos.artista.services.ArtistaService artistaService;

    public ComunidadeController(ComunidadeService service, 
                                br.ufpb.dcx.projetos.login.services.UsuarioService usuarioService,
                                br.ufpb.dcx.projetos.musica.services.MusicaService musicaService,
                                br.ufpb.dcx.projetos.album.services.AlbumService albumService,
                                br.ufpb.dcx.projetos.artista.services.ArtistaService artistaService) {
        this.service = service;
        this.usuarioService = usuarioService;
        this.musicaService = musicaService;
        this.albumService = albumService;
        this.artistaService = artistaService;
    }

    public ComunidadeController(ComunidadeService service, br.ufpb.dcx.projetos.login.services.UsuarioService usuarioService) {
        this(service, usuarioService, null, null, null);
    }

    public void registrarRotas(Javalin app) {
        app.get("/comunidade", this::feed, Role.USER);
        app.post("/comunidade/post", this::postar, Role.USER);
        app.post("/comunidade/post/compartilhar", this::compartilharItem, Role.USER);
        app.post("/comunidade/seguir/{id}", this::seguirUsuario, Role.USER);
        app.post("/comunidade/deseguir/{id}", this::deseguirUsuario, Role.USER);
        app.post("/comunidade/pedidos/{id}/aceitar", this::aceitarPedido, Role.USER);
        app.post("/comunidade/pedidos/{id}/recusar", this::recusarPedido, Role.USER);
        app.post("/comunidade/post/{id}/estrela", this::estrela, Role.USER);
        app.post("/comunidade/post/{id}/editar", this::editarPost, Role.USER);
        app.post("/comunidade/post/{id}/comentar", this::comentar, Role.USER);
        app.post("/comunidade/post/{id}/excluir", this::excluirPost, Role.USER);
        app.post("/comunidade/comentario/{id}/excluir", this::excluirComentario, Role.USER);
        app.get("/comunidade/post/{id}", this::detalhePost, Role.USER);
        app.get("/api/notificacoes", this::getNotificacoes, Role.USER);
        app.post("/api/notificacoes/ler-todas", this::lerTodasNotificacoes, Role.USER);
    }

    private void feed(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        
        ctx.sessionAttribute("comunidadeLastVisited", java.time.LocalDateTime.now());
        ctx.sessionAttribute("novidadesComunidade", false);

        var posts = service.listarFeedPublico(usuarioLogadoId);
        var amigos = service.listarAmigos(usuarioLogadoId);
        var pedidos = service.listarPedidosRecebidos(usuarioLogadoId);
        var seguidores = service.listarSeguidores(usuarioLogadoId);
        var seguindo = service.listarSeguindo(usuarioLogadoId);
        
        java.util.Set<String> amigosIds = amigos.stream()
                .map(br.ufpb.dcx.projetos.login.models.Usuario::getId)
                .collect(java.util.stream.Collectors.toSet());
        
        java.util.Set<String> seguindoIds = seguindo.stream()
                .map(br.ufpb.dcx.projetos.login.models.Usuario::getId)
                .collect(java.util.stream.Collectors.toSet());
        
        var postIds = posts.stream().map(PostViewDTO::id).toList();
        var comentarios = service.listarComentariosParaPosts(postIds);
        
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("usuarioLogadoId", usuarioLogadoId);
        modelo.put("paginaAtiva", "comunidade");
        modelo.put("posts", posts);
        modelo.put("amigos", amigos);
        modelo.put("amigosIds", amigosIds);
        modelo.put("seguindoIds", seguindoIds);
        modelo.put("pedidos", pedidos);
        modelo.put("seguidores", seguidores);
        modelo.put("seguindo", seguindo);
        modelo.put("usuarios", usuarioService.listarTodos());
        modelo.put("comentarios", comentarios);
        modelo.put("erro", ctx.sessionAttribute("erroComunidade")); // flash message

        // Carrega os rankings integrados para a aba de Rankings da Comunidade
        if (musicaService != null && albumService != null && artistaService != null) {
            var rankingController = new br.ufpb.dcx.projetos.rankings.RankingController(musicaService, albumService, artistaService);
            modelo.put("topMusicas", rankingController.getTopMusicas().stream().limit(10).toList());
            modelo.put("topAlbuns", rankingController.getTopAlbuns().stream().limit(10).toList());
            modelo.put("topArtistas", rankingController.getTopArtistas().stream().limit(10).toList());
        }

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
        
        redirecionarParaRefererOu(ctx);
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
        
        redirecionarParaRefererOu(ctx);
    }

    private void excluirPost(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String postId = ctx.pathParam("id");
        
        try {
            service.deletarPost(postId, usuarioLogadoId);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        
        redirecionarParaRefererOu(ctx);
    }

    private void editarPost(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String postId = ctx.pathParam("id");
        String novoConteudo = ctx.formParam("conteudo");

        try {
            service.editarPost(postId, usuarioLogadoId, novoConteudo);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        
        redirecionarParaRefererOu(ctx);
    }

    private void excluirComentario(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String comentarioId = ctx.pathParam("id");
        
        try {
            service.deletarComentario(comentarioId, usuarioLogadoId);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        
        redirecionarParaRefererOu(ctx);
    }

    private void estrela(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String postId = ctx.pathParam("id");
        
        try {
            service.alternarEstrela(postId, usuarioLogadoId);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        
        redirecionarParaRefererOu(ctx);
    }

    private void compartilharItem(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String tipo = ctx.formParam("tipo");
        String itemId = ctx.formParam("itemId");
        String conteudo = ctx.formParam("conteudo");
        
        try {
            service.criarPostCompartilhado(usuarioLogadoId, tipo, itemId, conteudo);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        
        ctx.redirect("/comunidade");
    }

    private void seguirUsuario(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String seguidoId = ctx.pathParam("id");
        
        try {
            service.enviarSolicitacao(usuarioLogadoId, seguidoId);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        
        redirecionarParaRefererOu(ctx);
    }

    private void deseguirUsuario(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String seguidoId = ctx.pathParam("id");
        
        try {
            service.deixarDeSeguir(usuarioLogadoId, seguidoId);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        
        redirecionarParaRefererOu(ctx);
    }

    private void redirecionarParaRefererOu(Context ctx) {
        String urlReferencia = ctx.header("Referer");
        ctx.redirect(urlReferencia != null ? urlReferencia : "/comunidade");
    }

    private void aceitarPedido(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String seguidorId = ctx.pathParam("id");
        try {
            service.aceitarSolicitacao(seguidorId, usuarioLogadoId);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        redirecionarParaRefererOu(ctx);
    }

    private void recusarPedido(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        String seguidorId = ctx.pathParam("id");
        try {
            service.recusarSolicitacao(seguidorId, usuarioLogadoId);
        } catch (IllegalArgumentException e) {
            ctx.sessionAttribute("erroComunidade", e.getMessage());
        }
        redirecionarParaRefererOu(ctx);
    }

    private void getNotificacoes(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        if (usuarioLogadoId == null) {
            ctx.status(401).json(List.of());
            return;
        }
        ctx.json(service.listarNotificacoes(usuarioLogadoId));
    }

    private void lerTodasNotificacoes(Context ctx) {
        String usuarioLogadoId = ctx.sessionAttribute("usuarioLogadoId");
        if (usuarioLogadoId == null) {
            ctx.status(401);
            return;
        }
        service.marcarNotificacoesComoLidas(usuarioLogadoId);
        ctx.status(200);
    }
}
