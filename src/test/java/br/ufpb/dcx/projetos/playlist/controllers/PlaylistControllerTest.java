package br.ufpb.dcx.projetos.playlist.controllers;

import br.ufpb.dcx.projetos.playlist.models.Playlist;
import br.ufpb.dcx.projetos.playlist.services.PlaylistService;
import io.javalin.Javalin;
import io.javalin.testtools.JavalinTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PlaylistControllerTest {

    @Mock
    private PlaylistService playlistService;

    private Javalin criarApp() {
        var app = Javalin.create();
        PlaylistController controller = new PlaylistController(playlistService);
        // Simular sessão
        app.before(ctx -> {
            ctx.sessionAttribute("usuarioLogadoId", "user123");
            ctx.sessionAttribute("usuarioLogado", "User Teste");
        });
        
        // Registrar as rotas
        app.get("/playlists", controller::listar);
        app.post("/playlists", controller::criarPlaylist);
        app.post("/playlists/{id}/excluir", controller::excluirPlaylist);
        return app;
    }

    @Test
    void deveListarPlaylists() {
        when(playlistService.findByUsuarioId(anyString())).thenReturn(Collections.emptyList());

        JavalinTest.test(criarApp(), (server, client) -> {
            try (var resposta = client.get("/playlists")) {
                assertEquals(200, resposta.code());
            }
        });
    }
}
