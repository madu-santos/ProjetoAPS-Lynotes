package br.ufpb.dcx.projetos.login.controllers;

import br.ufpb.dcx.projetos.login.dto.UsuarioDTO;
import br.ufpb.dcx.projetos.login.models.Usuario;
import br.ufpb.dcx.projetos.login.services.UsuarioService;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;
import br.ufpb.dcx.projetos.login.models.Role;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.album.models.Album;
import java.util.Map;
import java.util.Optional;

public final class UsuarioController {

    private final UsuarioService service;
    private final MusicaService musicaService;
    private final ArtistaService artistaService;
    private final AlbumService albumService;
    private final br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService;
    private final br.ufpb.dcx.projetos.playlist.services.PlaylistService playlistService;

    public UsuarioController(UsuarioService service, MusicaService musicaService, ArtistaService artistaService, AlbumService albumService, br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService, br.ufpb.dcx.projetos.playlist.services.PlaylistService playlistService) {
        this.service = service;
        this.musicaService = musicaService;
        this.artistaService = artistaService;
        this.albumService = albumService;
        this.comunidadeService = comunidadeService;
        this.playlistService = playlistService;
    }

    public void registrarRotas(Javalin app) {
        app.get("/usuarios", this::listar, Role.USER);
        app.get("/usuarios/novo", this::exibirCadastro, Role.ANYONE);
        app.post("/usuarios", this::cadastrar, Role.ANYONE);
        app.get("/usuarios/me", this::exibirMeuPerfil, Role.USER);
        app.get("/perfil", this::exibirMeuPerfil, Role.USER);
        app.post("/usuarios/me", this::salvarMeuPerfil, Role.USER);
        app.post("/perfil", this::salvarMeuPerfil, Role.USER);
        app.get("/usuarios/me/check-username", this::verificarDisponibilidadeUsernameEndpoint, Role.USER);
        app.post("/usuarios/me/excluir", this::excluirMinhaConta, Role.USER);
        app.get("/usuarios/{id}", this::exibirPerfilPublico, Role.USER);
        app.get("/perfil/{id}", this::exibirPerfilPublico, Role.USER);
        app.get("/usuarios/{id}/editar", this::exibirEdicao, Role.USER);
        app.post("/usuarios/{id}", this::atualizar, Role.USER);
        app.post("/usuarios/{id}/excluir", this::excluir, Role.USER);

        app.get("/recuperar", this::exibirRecuperacao, Role.ANYONE);
        app.post("/recuperar/solicitar", this::solicitarRecuperacao, Role.ANYONE);
        app.post("/recuperar/confirmar", this::confirmarRecuperacao, Role.ANYONE);
    }

    private void listar(Context ctx) {
        String q = ctx.queryParam("q");
        List<Usuario> usuarios = (q != null && !q.trim().isEmpty()) ? service.buscar(q) : service.listarTodos();

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("usuarioLogadoId", ctx.sessionAttribute("usuarioLogadoId"));
        modelo.put("usuarioLogadoTipo", ctx.sessionAttribute("usuarioLogadoTipo"));
        modelo.put("listaUsuarios", usuarios);
        modelo.put("semUsuarios", usuarios.isEmpty());
        modelo.put("q", q);
        ctx.render("usuarios/lista", modelo);
    }

    private void exibirCadastro(Context ctx) {
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("edicao", false);
        modelo.put("acao", "/usuarios");
        modelo.put("nome", "");
        modelo.put("username", "");
        modelo.put("email", "");
        modelo.put("bio", "");
        modelo.put("fotoUrl", "");
        ctx.render("usuarios/formulario", modelo);
    }

    private void cadastrar(Context ctx) {
        UsuarioDTO dto = new UsuarioDTO(
                ctx.formParam("nome"),
                ctx.formParam("username"),
                ctx.formParam("email"),
                ctx.formParam("senha"),
                ctx.formParam("bio"),
                ctx.formParam("fotoUrl")
        ).normalizado();

        try {
            dto.validarParaCriacao();
            Usuario usuario = new Usuario(dto.nome(), dto.username(), dto.email(), dto.senha());
            usuario.setBio(dto.bio());
            usuario.setFotoUrl(dto.fotoUrl());
            service.cadastrarUsuario(usuario);
            if (playlistService != null) {
                playlistService.findOrCreateHiddenPlaylist(usuario.getId());
            }
            if (ctx.sessionAttribute("usuarioLogado") != null) {
                ctx.redirect("/usuarios");
            } else {
                ctx.sessionAttribute("usuarioLogado", usuario.getNome());
                ctx.sessionAttribute("usuarioLogadoId", usuario.getId());
                ctx.sessionAttribute("usuarioLogadoTipo", usuario.getTipo());
                ctx.redirect("/");
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("edicao", false);
            modelo.put("acao", "/usuarios");
            modelo.put("nome", dto.nome());
            modelo.put("username", dto.username());
            modelo.put("email", dto.email());
            modelo.put("bio", dto.bio());
            modelo.put("fotoUrl", dto.fotoUrl());
            modelo.put("erro", e.getMessage());
            ctx.status(HttpStatus.OK).render("usuarios/formulario", modelo);
        }
    }

    private void exibirPerfilPublico(Context ctx) {
        String id = ctx.pathParam("id");
        String logadoId = (String) ctx.sessionAttribute("usuarioLogadoId");

        if (id.equals(logadoId)) {
            ctx.redirect("/");
            return;
        }

        Optional<Usuario> usuarioOpt = service.buscarPorId(id);
        if (usuarioOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Usuário não encontrado");
            return;
        }

        Usuario usuario = usuarioOpt.get();
        List<Musica> musicas = musicaService.findByUsuarioId(id);
        List<br.ufpb.dcx.projetos.artista.models.Artista> artistas = artistaService.listar("", id);
        List<br.ufpb.dcx.projetos.album.models.Album> albums = albumService.findAll(id);
        List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService.findAllByUsuarioId(id);
        
        List<br.ufpb.dcx.projetos.comunidade.views.PostViewDTO> userPosts = comunidadeService.listarFeed(logadoId, id);

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("usuarioLogadoId", logadoId);
        modelo.put("perfilId", id);
        modelo.put("perfilNome", usuario.getNome());
        modelo.put("perfilUsername", usuario.getUsername());
        modelo.put("perfilFotoUrl", usuario.getFotoUrl() != null ? usuario.getFotoUrl() : "");
        
        boolean amigos = false;
        boolean pedidoPendenteEnviado = false;
        boolean pedidoPendenteRecebido = false;
        
        if (comunidadeService != null && logadoId != null) {
            amigos = comunidadeService.isSeguindo(logadoId, id) && comunidadeService.isSeguindo(id, logadoId);
            pedidoPendenteEnviado = comunidadeService.isPedidoPendente(logadoId, id);
            pedidoPendenteRecebido = comunidadeService.isPedidoPendente(id, logadoId);
        }
        
        if (comunidadeService != null) {
            modelo.put("seguidores", comunidadeService.listarSeguidores(id));
            modelo.put("seguindo", comunidadeService.listarSeguindo(id));
        } else {
            modelo.put("seguidores", List.of());
            modelo.put("seguindo", List.of());
        }

        modelo.put("amigos", amigos);
        modelo.put("pedidoPendenteEnviado", pedidoPendenteEnviado);
        modelo.put("pedidoPendenteRecebido", pedidoPendenteRecebido);
        modelo.put("isMe", false);
        
        modelo.put("userPosts", userPosts);
        modelo.put("semPosts", userPosts.isEmpty());
        
        modelo.put("musicas", musicas);
        modelo.put("semMusicas", musicas.isEmpty());
        
        modelo.put("artistas", artistas);
        modelo.put("semArtistas", artistas.isEmpty());
        
        modelo.put("albums", albums);
        modelo.put("semAlbums", albums.isEmpty());
        
        modelo.put("playlists", playlists);
        modelo.put("semPlaylists", playlists.isEmpty());
        
        ctx.render("perfil", modelo);
    }

    private void exibirEdicao(Context ctx) {
        String idLogado = ctx.sessionAttribute("usuarioLogadoId");
        String tipoLogado = ctx.sessionAttribute("usuarioLogadoTipo");
        String id = ctx.pathParam("id");

        if (idLogado == null || !idLogado.equals(id)) {
            ctx.status(HttpStatus.FORBIDDEN)
                    .result("Acesso negado: Apenas o próprio usuário pode editar esta conta.");
            return;
        }

        Optional<Usuario> usuarioOpt = service.buscarPorId(id);
        if (usuarioOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Usuário não encontrado");
            return;
        }

        Usuario usuario = usuarioOpt.get();
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("edicao", true);
        modelo.put("acao", "/usuarios/" + id);
        modelo.put("nome", usuario.getNome());
        modelo.put("username", usuario.getUsername());
        modelo.put("email", usuario.getEmail());
        modelo.put("bio", usuario.getBio());
        modelo.put("fotoUrl", usuario.getFotoUrl() != null ? usuario.getFotoUrl() : "");
        ctx.render("usuarios/formulario", modelo);
    }

    private void atualizar(Context ctx) {
        String idLogado = ctx.sessionAttribute("usuarioLogadoId");
        String id = ctx.pathParam("id");

        if (idLogado == null || !idLogado.equals(id)) {
            ctx.status(HttpStatus.FORBIDDEN)
                    .result("Acesso negado: Apenas o próprio usuário pode atualizar esta conta.");
            return;
        }

        UsuarioDTO dto = new UsuarioDTO(
                ctx.formParam("nome"),
                ctx.formParam("username"),
                ctx.formParam("email"),
                ctx.formParam("senha"),
                ctx.formParam("bio"),
                ctx.formParam("fotoUrl")
        ).normalizado();

        try {
            dto.validarParaAtualizacao();

            Optional<Usuario> usuarioOpt = service.buscarPorId(id);
            if (usuarioOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND).result("Usuário não encontrado");
                return;
            }

            Usuario usuarioAntigo = usuarioOpt.get();
            String senhaFinal = (dto.senha() == null || dto.senha().trim().isEmpty()) ? usuarioAntigo.getSenha() : dto.senha();

            Usuario usuarioAtualizado = new Usuario(id, dto.nome(), dto.username(), dto.email(), senhaFinal, usuarioAntigo.getTipo(), dto.bio(), dto.fotoUrl());

            service.cadastrarUsuario(usuarioAtualizado);
            ctx.redirect("/usuarios");
        } catch (IllegalArgumentException e) {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("edicao", true);
            modelo.put("acao", "/usuarios/" + id);
            modelo.put("nome", dto.nome());
            modelo.put("username", dto.username());
            modelo.put("email", dto.email());
            modelo.put("bio", dto.bio());
            modelo.put("fotoUrl", dto.fotoUrl());
            modelo.put("erro", e.getMessage());
            ctx.status(HttpStatus.OK).render("usuarios/formulario", modelo);
        }
    }

    private void excluir(Context ctx) {
        String idLogado = ctx.sessionAttribute("usuarioLogadoId");
        String id = ctx.pathParam("id");

        if (idLogado == null || !idLogado.equals(id)) {
            ctx.status(HttpStatus.FORBIDDEN)
                    .result("Acesso negado: Apenas o próprio usuário pode apagar esta conta.");
            return;
        }

        service.remover(id);

        ctx.req().getSession().invalidate();
        ctx.redirect("/login");
    }

    private void exibirMeuPerfil(Context ctx) {
        String idLogado = ctx.sessionAttribute("usuarioLogadoId");
        if (idLogado == null) {
            ctx.redirect("/login");
            return;
        }

        Optional<Usuario> usuarioOpt = service.buscarPorId(idLogado);
        if (usuarioOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Usuário não encontrado");
            return;
        }

        Usuario usuario = usuarioOpt.get();
        
        List<Musica> minhasMusicas = musicaService != null ? musicaService.findByUsuarioId(idLogado) : List.of();
        List<Artista> meusArtistas = artistaService != null ? artistaService.listar(null, idLogado) : List.of();
        List<Album> meusAlbums = albumService != null ? albumService.findAll(idLogado) : List.of();
        List<br.ufpb.dcx.projetos.playlist.models.Playlist> minhasPlaylists = playlistService != null ? playlistService.findAllByUsuarioId(idLogado) : List.of();
        List<br.ufpb.dcx.projetos.comunidade.views.PostViewDTO> meusPosts = comunidadeService != null ? comunidadeService.listarFeed(idLogado, idLogado) : List.of();

        var comentariosPosts = new java.util.HashMap<String, java.util.List<br.ufpb.dcx.projetos.comunidade.views.ComentarioViewDTO>>();
        if (comunidadeService != null) {
            for (var p : meusPosts) {
                comentariosPosts.put(p.id(), comunidadeService.listarComentarios(p.id()));
            }
        }

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("usuarioLogadoId", idLogado);
        modelo.put("paginaAtiva", "perfil");
        modelo.put("nome", usuario.getNome());
        modelo.put("username", usuario.getUsername());
        modelo.put("email", usuario.getEmail());
        modelo.put("bio", usuario.getBio());
        modelo.put("fotoUrl", usuario.getFotoUrl() != null ? usuario.getFotoUrl() : "");
        modelo.put("sucesso", ctx.queryParam("sucesso") != null);
        if (comunidadeService != null) {
            modelo.put("seguidores", comunidadeService.listarSeguidores(idLogado));
            modelo.put("seguindo", comunidadeService.listarSeguindo(idLogado));
        } else {
            modelo.put("seguidores", List.of());
            modelo.put("seguindo", List.of());
        }

        modelo.put("minhasMusicas", minhasMusicas);
        modelo.put("meusArtistas", meusArtistas);
        modelo.put("meusAlbums", meusAlbums);
        modelo.put("minhasPlaylists", minhasPlaylists);
        modelo.put("meusPosts", meusPosts);
        modelo.put("comentariosPosts", comentariosPosts);
        ctx.render("usuarios/me", modelo);
    }

    private void salvarMeuPerfil(Context ctx) {
        String idLogado = ctx.sessionAttribute("usuarioLogadoId");
        if (idLogado == null) {
            ctx.redirect("/login");
            return;
        }

        UsuarioDTO dto = new UsuarioDTO(
                ctx.formParam("nome"),
                ctx.formParam("username"),
                ctx.formParam("email"),
                ctx.formParam("senha"),
                ctx.formParam("bio"),
                ctx.formParam("fotoUrl")
        ).normalizado();

        try {
            dto.validarParaAtualizacao();

            Optional<Usuario> usuarioOpt = service.buscarPorId(idLogado);
            if (usuarioOpt.isEmpty()) {
                ctx.status(HttpStatus.NOT_FOUND).result("Usuário não encontrado");
                return;
            }

            Usuario usuarioAntigo = usuarioOpt.get();
            String senhaFinal = (dto.senha() == null || dto.senha().trim().isEmpty()) ? usuarioAntigo.getSenha() : dto.senha();

            Usuario usuarioAtualizado = new Usuario(idLogado, dto.nome(), dto.username(), dto.email(), senhaFinal, usuarioAntigo.getTipo(), dto.bio(), dto.fotoUrl());

            service.cadastrarUsuario(usuarioAtualizado);

            // Atualiza o nome logado na sessão caso tenha mudado
            ctx.sessionAttribute("usuarioLogado", usuarioAtualizado.getNome());

            ctx.redirect("/usuarios/me?sucesso=true");
        } catch (IllegalArgumentException e) {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("nome", dto.nome());
            modelo.put("username", dto.username());
            modelo.put("email", dto.email());
            modelo.put("bio", dto.bio());
            modelo.put("fotoUrl", dto.fotoUrl());
            modelo.put("erro", e.getMessage());
            ctx.status(HttpStatus.OK).render("usuarios/me", modelo);
        }
    }

    private void verificarDisponibilidadeUsernameEndpoint(Context ctx) {
        String username = ctx.queryParam("username");
        String idLogado = ctx.sessionAttribute("usuarioLogadoId");

        if (username == null || username.trim().isEmpty()) {
            ctx.html("<span class='text-danger small'>Username inválido</span>");
            return;
        }

        Optional<Usuario> existente = service.buscarPorUsername(username.trim());
        if (existente.isEmpty() || existente.get().getId().equals(idLogado)) {
            ctx.html("<span class='text-success small'>✓ Username disponível</span>");
        } else {
            ctx.html("<span class='text-danger small'>✗ Username já está em uso</span>");
        }
    }

    private void excluirMinhaConta(Context ctx) {
        String idLogado = ctx.sessionAttribute("usuarioLogadoId");
        if (idLogado == null) {
            ctx.redirect("/login");
            return;
        }

        service.remover(idLogado);
        ctx.req().getSession().invalidate();
        ctx.redirect("/login?excluido=true");
    }

    private void exibirRecuperacao(Context ctx) {
        try {
            if (ctx.req().getSession(false) != null) {
                ctx.req().getSession().invalidate();
            }
        } catch (Exception ignored) {}
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("step", 1);
        ctx.render("recuperacao", modelo);
    }

    private void solicitarRecuperacao(Context ctx) {
        try {
            if (ctx.req().getSession(false) != null) {
                ctx.req().getSession().invalidate();
            }
        } catch (Exception ignored) {}
        String email = ctx.formParam("email");
        Map<String, Object> modelo = new HashMap<>();
        try {
            service.solicitarRecuperacao(email);
            modelo.put("step", 2);
            modelo.put("email", email);
            ctx.render("recuperacao", modelo);
        } catch (IllegalArgumentException e) {
            modelo.put("step", 1);
            modelo.put("erro", e.getMessage());
            modelo.put("email", email);
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).render("recuperacao", modelo);
        }
    }

    private void confirmarRecuperacao(Context ctx) {
        String email = ctx.formParam("email");
        String codigo = ctx.formParam("codigo");
        String novaSenha = ctx.formParam("novaSenha");
        String confirmacao = ctx.formParam("confirmacaoSenha");

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("email", email);
        modelo.put("codigo", codigo);

        if (novaSenha == null || confirmacao == null || !novaSenha.equals(confirmacao)) {
            modelo.put("step", 2);
            modelo.put("erro", "As senhas não coincidem.");
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).render("recuperacao", modelo);
            return;
        }

        try {
            service.confirmarRecuperacao(email, codigo, novaSenha);
            ctx.redirect("/login?recuperado=true");
        } catch (IllegalArgumentException e) {
            modelo.put("step", 2);
            modelo.put("erro", e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).render("recuperacao", modelo);
        }
    }
}
