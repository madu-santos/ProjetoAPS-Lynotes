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

    @Mock
    private br.ufpb.dcx.projetos.playlist.services.PlaylistService playlistService;

    @Mock
    private br.ufpb.dcx.projetos.artista.services.ArtistaService artistaService;

    @Mock
    private br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService;

    private Javalin criarApp() {
        var app = Javalin.create(config ->
                config.fileRenderer(new io.javalin.rendering.template.JavalinThymeleaf(templateEngine())));
        MusicaController controller = new MusicaController(musicaService, albumService, playlistService, artistaService, comunidadeService);
        
        app.before(ctx -> {
            ctx.sessionAttribute("usuarioLogadoId", "user123");
            ctx.sessionAttribute("usuarioLogado", "User Teste");
        });
        
        app.get("/musicas", controller::listar);
        return app;
    }

    private org.thymeleaf.TemplateEngine templateEngine() {
        org.thymeleaf.templateresolver.ClassLoaderTemplateResolver resolver = new org.thymeleaf.templateresolver.ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        org.thymeleaf.TemplateEngine engine = new org.thymeleaf.TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Test
    void deveListarMusicas() {
        when(musicaService.search(any(), anyString())).thenReturn(Collections.emptyList());

        JavalinTest.test(criarApp(), (server, client) -> {
            try (var resposta = client.get("/musicas")) {
                assertEquals(200, resposta.code());
            }
        });
    }
}
