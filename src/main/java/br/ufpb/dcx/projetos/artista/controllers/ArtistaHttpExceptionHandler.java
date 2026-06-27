package br.ufpb.dcx.projetos.artista.controllers;

import br.ufpb.dcx.projetos.artista.exceptions.ArtistaIdInvalidoException;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaNaoEncontradoException;
import br.ufpb.dcx.projetos.infra.database.DatabaseException;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ArtistaHttpExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ArtistaHttpExceptionHandler.class);

    private ArtistaHttpExceptionHandler() {
    }

    public static void registrar(Javalin app) {
        app.exception(ArtistaIdInvalidoException.class, (erro, ctx) -> {
            LOGGER.warn("Requisição com ID de artista inválido. path={} mensagem={}",
                    ctx.path(), erro.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST).result(erro.getMessage());
        });

        app.exception(ArtistaNaoEncontradoException.class, (erro, ctx) -> {
            LOGGER.warn("Artista não encontrado. path={} mensagem={}",
                    ctx.path(), erro.getMessage());
            ctx.status(HttpStatus.NOT_FOUND).result(erro.getMessage());
        });

        app.exception(DatabaseException.class, (erro, ctx) -> {
            LOGGER.error("Falha de persistência no domínio de artistas. path={}",
                    ctx.path(), erro);
            String mensagem = ctx.path().startsWith("/artistas")
                    ? "Não foi possível concluir a operação com artistas."
                    : "Não foi possível concluir a operação solicitada.";
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .result(mensagem);
        });
    }
}
