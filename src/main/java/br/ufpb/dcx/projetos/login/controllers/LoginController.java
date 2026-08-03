package br.ufpb.dcx.projetos.login.controllers;

import br.ufpb.dcx.projetos.login.models.Role;
import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;
import java.util.Objects;

public class LoginController {

    public static final String ATRIBUTO_USUARIO_SESSAO = "usuarioLogado";

    private final br.ufpb.dcx.projetos.login.services.UsuarioService service;

    public LoginController(br.ufpb.dcx.projetos.login.services.UsuarioService service) {
        this.service = service;
    }

    public void registrarRotas(Javalin app) {
        app.get("/login", this::exibirLogin, Role.ANYONE);
        app.post("/login", this::autenticar, Role.ANYONE);
        app.post("/logout", this::sair, Role.ANYONE);
    }

    public static boolean estaAutenticado(Context ctx) {
        return Objects.nonNull(ctx.sessionAttribute(ATRIBUTO_USUARIO_SESSAO));
    }

    private void exibirLogin(Context ctx) {
        if (estaAutenticado(ctx)) {
            ctx.redirect("/artistas");
            return;
        }

        ctx.render("login", Map.of());
    }

    private void autenticar(Context ctx) {
        String identificador = ctx.formParam("usuario");
        String senha = ctx.formParam("senha");

        if (service.verificarCredenciais(identificador, senha)) {
            br.ufpb.dcx.projetos.login.models.Usuario usuario = service.buscarPorEmail(identificador)
                    .or(() -> service.buscarPorUsername(identificador))
                    .orElseThrow();
            ctx.sessionAttribute(ATRIBUTO_USUARIO_SESSAO, usuario.getNome());
            ctx.sessionAttribute("usuarioLogadoId", usuario.getId());
            ctx.sessionAttribute("usuarioLogadoTipo", usuario.getTipo());
            ctx.redirect("/");
            return;
        }

        ctx.status(401).render("login", Map.of(
                "erro", "Usuário ou senha inválidos.",
                "usuarioInformado", Objects.toString(identificador, "")
        ));
    }

    private void sair(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.redirect("/");
    }
}
