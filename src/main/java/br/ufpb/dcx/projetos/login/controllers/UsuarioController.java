package br.ufpb.dcx.projetos.login.controllers;

import br.ufpb.dcx.projetos.login.models.Usuario;
import br.ufpb.dcx.projetos.login.services.UsuarioService;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class UsuarioController {

    private final UsuarioService service;
    private final MusicaService musicaService;

    public UsuarioController(UsuarioService service, MusicaService musicaService) {
        this.service = service;
        this.musicaService = musicaService;
    }

    public void registrarRotas(Javalin app) {
        app.get("/usuarios", this::listar);
        app.get("/usuarios/novo", this::exibirCadastro);
        app.post("/usuarios", this::cadastrar);
        app.get("/usuarios/{id}", this::exibirPerfilPublico);
        app.get("/usuarios/{id}/editar", this::exibirEdicao);
        app.post("/usuarios/{id}", this::atualizar);
        app.post("/usuarios/{id}/excluir", this::excluir);
    }

    private void listar(Context ctx) {
        List<Usuario> usuarios = service.listarTodos();
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("listaUsuarios", usuarios);
        modelo.put("semUsuarios", usuarios.isEmpty());
        ctx.render("usuarios/lista", modelo);
    }

    private void exibirCadastro(Context ctx) {
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("edicao", false);
        modelo.put("acao", "/usuarios");
        modelo.put("nome", "");
        modelo.put("email", "");
        ctx.render("usuarios/formulario", modelo);
    }

    private void cadastrar(Context ctx) {
        String nome = ctx.formParam("nome");
        String email = ctx.formParam("email");
        String senha = ctx.formParam("senha");

        Usuario usuario = new Usuario(nome, email, senha);

        try {
            service.cadastrarUsuario(usuario);
            if (ctx.sessionAttribute("usuarioLogado") != null) {
                ctx.redirect("/usuarios");
            } else {
                ctx.redirect("/login");
            }
        } catch (IllegalArgumentException e) {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("edicao", false);
            modelo.put("acao", "/usuarios");
            modelo.put("nome", nome);
            modelo.put("email", email);
            modelo.put("erro", e.getMessage());
            ctx.status(HttpStatus.OK).render("usuarios/formulario", modelo);
        }
    }

    private void exibirPerfilPublico(Context ctx) {
        String id = ctx.pathParam("id");
        Optional<Usuario> usuarioOpt = service.buscarPorId(id);

        if (usuarioOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Usuário não encontrado");
            return;
        }

        Usuario usuario = usuarioOpt.get();
        List<Musica> musicas = musicaService.findByUsuarioId(id);

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("perfilNome", usuario.getNome());
        modelo.put("musicas", musicas);
        modelo.put("semMusicas", musicas.isEmpty());
        ctx.render("perfil", modelo);
    }

    private void exibirEdicao(Context ctx) {
        String id = ctx.pathParam("id");
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
        modelo.put("email", usuario.getEmail());
        ctx.render("usuarios/formulario", modelo);
    }

    private void atualizar(Context ctx) {
        String id = ctx.pathParam("id");
        String nome = ctx.formParam("nome");
        String email = ctx.formParam("email");
        String senha = ctx.formParam("senha");

        Optional<Usuario> usuarioOpt = service.buscarPorId(id);
        if (usuarioOpt.isEmpty()) {
            ctx.status(HttpStatus.NOT_FOUND).result("Usuário não encontrado");
            return;
        }

        Usuario usuarioAntigo = usuarioOpt.get();
        String senhaFinal = (senha == null || senha.trim().isEmpty()) ? usuarioAntigo.getSenha() : senha;

        Usuario usuarioAtualizado = new Usuario(id, nome, email, senhaFinal, usuarioAntigo.getTipo());

        try {
            service.cadastrarUsuario(usuarioAtualizado);
            ctx.redirect("/usuarios");
        } catch (IllegalArgumentException e) {
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
            modelo.put("edicao", true);
            modelo.put("acao", "/usuarios/" + id);
            modelo.put("nome", nome);
            modelo.put("email", email);
            modelo.put("erro", e.getMessage());
            ctx.status(HttpStatus.OK).render("usuarios/formulario", modelo);
        }
    }

    private void excluir(Context ctx) {
        String id = ctx.pathParam("id");
        service.remover(id);
        ctx.redirect("/usuarios");
    }
}
