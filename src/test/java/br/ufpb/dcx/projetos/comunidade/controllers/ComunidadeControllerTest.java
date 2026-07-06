package br.ufpb.dcx.projetos.comunidade.controllers;

import br.ufpb.dcx.projetos.comunidade.services.ComunidadeService;
import br.ufpb.dcx.projetos.login.services.UsuarioService;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComunidadeControllerTest {

    @Mock
    private ComunidadeService comunidadeService;

    @Mock
    private UsuarioService usuarioService;

    private Javalin criarApp() {
        var app = Javalin.create();
        ComunidadeController controller = new ComunidadeController(comunidadeService, usuarioService);
        
        app.before(ctx -> {
            ctx.sessionAttribute("usuarioLogadoId", "user123");
            ctx.sessionAttribute("usuarioLogado", "User Teste");
        });
        
        app.get("/comunidade", ctx -> controller.registrarRotas(app));
        controller.registrarRotas(app);
        return app;
    }

    @Test
    void deveExibirFeedDaComunidade() {
        when(comunidadeService.listarFeed(anyString(), isNull())).thenReturn(Collections.emptyList());
        when(usuarioService.listarTodos()).thenReturn(Collections.emptyList());

        JavalinTest.test(criarApp(), (server, client) -> {
            try (var resposta = client.get("/comunidade")) {
                assertEquals(200, resposta.code());
            }
        });
    }
}
