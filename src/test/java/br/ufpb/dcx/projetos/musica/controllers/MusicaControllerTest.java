package br.ufpb.dcx.projetos.musica.controllers;

import br.ufpb.dcx.projetos.musica.services.MusicaService;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MusicaControllerTest {

    @Mock
    private MusicaService musicaService;

    @Mock
    private AlbumService albumService;

    private Javalin criarApp() {
        var app = Javalin.create();
        MusicaController controller = new MusicaController(musicaService, albumService);
        
        app.before(ctx -> {
            ctx.sessionAttribute("usuarioLogadoId", "user123");
            ctx.sessionAttribute("usuarioLogado", "User Teste");
        });
        
        app.get("/musicas", controller::listar);
        return app;
    }

    @Test
    void deveListarMusicas() {
        when(musicaService.findAll()).thenReturn(Collections.emptyList());

        JavalinTest.test(criarApp(), (server, client) -> {
            try (var resposta = client.get("/musicas")) {
                assertEquals(200, resposta.code());
            }
        });
    }
}
