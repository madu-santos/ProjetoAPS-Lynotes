package br.ufpb.dcx.projetos.exceptions;

import br.ufpb.dcx.projetos.artista.exceptions.ArtistaIdInvalidoException;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaNaoEncontradoException;
import br.ufpb.dcx.projetos.infra.database.DatabaseException;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlobalHttpExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalHttpExceptionHandler.class);

    private GlobalHttpExceptionHandler() {
    }

    public static void registrar(Javalin app) {
        // Artista
        app.exception(ArtistaIdInvalidoException.class, (erro, ctx) -> {
            LOGGER.warn("ID inválido: {}", erro.getMessage());
            ctx.status(HttpStatus.BAD_REQUEST).result(erro.getMessage());
        });

        app.exception(ArtistaNaoEncontradoException.class, (erro, ctx) -> {
            LOGGER.warn("Recurso não encontrado: {}", erro.getMessage());
            ctx.status(HttpStatus.NOT_FOUND).result(erro.getMessage());
        });

        // Common
        app.exception(ResourceNotFoundException.class, (erro, ctx) -> {
            LOGGER.warn("Recurso não encontrado: {}", erro.getMessage());
            ctx.status(HttpStatus.NOT_FOUND).result(erro.getMessage());
        });

        app.exception(IllegalArgumentException.class, (erro, ctx) -> {
            LOGGER.warn("Argumento inválido: {}", erro.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT).result(erro.getMessage());
        });

        // Database
        app.exception(DatabaseException.class, (erro, ctx) -> {
            LOGGER.error("Erro de banco de dados: {}", erro.getMessage(), erro);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .result("Não foi possível concluir a operação de forma segura. Detalhes ocultados.");
        });

        // Javalin HTTP Responses
        app.exception(io.javalin.http.HttpResponseException.class, (erro, ctx) -> {
            ctx.status(erro.getStatus());
            if (erro instanceof io.javalin.http.RedirectResponse) {
                ctx.redirect(erro.getMessage());
            } else {
                ctx.result(erro.getMessage());
            }
        });

        // General
        app.exception(Exception.class, (erro, ctx) -> {
            LOGGER.error("Erro não tratado: {}", erro.getMessage(), erro);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .result("Ocorreu um erro inesperado no sistema.");
        });
    }
}
