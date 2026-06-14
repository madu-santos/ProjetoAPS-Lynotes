package br.ufpb.dcx.projetos.login.controllers;

import io.javalin.Javalin;
import io.javalin.http.Context;

import java.util.Map;
import java.util.Objects;

public class LoginController {

    public static final String ATRIBUTO_USUARIO_SESSAO = "usuarioLogado";

    private static final String USUARIO_TESTE = "admin";
    private static final String SENHA_TESTE = "123456";

    public void registrarRotas(Javalin app) {
        app.get("/login", this::exibirLogin);
        app.post("/login", this::autenticar);
        app.post("/logout", this::sair);
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
        String usuario = ctx.formParam("usuario");
        String senha = ctx.formParam("senha");

        if (USUARIO_TESTE.equals(usuario) && SENHA_TESTE.equals(senha)) {
            ctx.sessionAttribute(ATRIBUTO_USUARIO_SESSAO, usuario);
            ctx.redirect("/artistas");
            return;
        }

        ctx.status(401).render("login", Map.of(
                "erro", "Usuário ou senha inválidos.",
                "usuarioInformado", Objects.toString(usuario, "")
        ));
    }

    private void sair(Context ctx) {
        ctx.req().getSession().invalidate();
        ctx.redirect("/login");
    }
}
