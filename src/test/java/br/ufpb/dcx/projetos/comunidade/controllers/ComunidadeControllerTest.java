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
        var app = Javalin.create(config ->
                config.fileRenderer(new io.javalin.rendering.template.JavalinThymeleaf(templateEngine())));
        ComunidadeController controller = new ComunidadeController(comunidadeService, usuarioService);
        
        app.before(ctx -> {
            ctx.sessionAttribute("usuarioLogadoId", "user123");
            ctx.sessionAttribute("usuarioLogado", "User Teste");
        });
        
        controller.registrarRotas(app);
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
    void deveExibirFeedDaComunidade() {
        when(comunidadeService.listarFeedPublico(anyString())).thenReturn(Collections.emptyList());
        when(comunidadeService.listarAmigos(anyString())).thenReturn(Collections.emptyList());
        when(comunidadeService.listarPedidosRecebidos(anyString())).thenReturn(Collections.emptyList());

        JavalinTest.test(criarApp(), (server, client) -> {
            try (var resposta = client.get("/comunidade")) {
                assertEquals(200, resposta.code());
            }
        });
    }

    @Test
    void deveCompartilharItemComSucesso() {
        JavalinTest.test(criarApp(), (server, client) -> {
            var form = "tipo=SHARE_MUSIC&itemId=musica1&conteudo=Excelente música!";
            try (var resposta = client.post("/comunidade/post/compartilhar", form)) {
                assertEquals(200, resposta.code());
                verify(comunidadeService, times(1)).criarPostCompartilhado("user123", "SHARE_MUSIC", "musica1", "Excelente música!");
            }
        });
    }

    @Test
    void deveSeguirUsuarioComSucesso() {
        JavalinTest.test(criarApp(), (server, client) -> {
            try (var resposta = client.post("/comunidade/seguir/user456")) {
                assertEquals(200, resposta.code());
                verify(comunidadeService, times(1)).enviarSolicitacao("user123", "user456");
            }
        });
    }

    @Test
    void deveDeseguirUsuarioComSucesso() {
        JavalinTest.test(criarApp(), (server, client) -> {
            try (var resposta = client.post("/comunidade/deseguir/user456")) {
                assertEquals(200, resposta.code());
                verify(comunidadeService, times(1)).desfazerAmizade("user123", "user456");
            }
        });
    }
}
