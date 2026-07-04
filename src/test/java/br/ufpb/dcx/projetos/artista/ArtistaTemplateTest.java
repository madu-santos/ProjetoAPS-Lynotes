package br.ufpb.dcx.projetos.artista;

import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.views.ArtistaDTOView;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtistaTemplateTest {

    @Test
    void deveRenderizarListaComArtista() {
        TemplateEngine engine = criarTemplateEngine();
        Context contexto = new Context();
        contexto.setVariable("usuarioLogado", "admin");
        contexto.setVariable("semArtistas", false);
        contexto.setVariable("busca", "");
        contexto.setVariable("buscaAtiva", false);
        contexto.setVariable("listaArtistas",
                List.of(Artista.novo("Djavan", "MPB", "Cantor e compositor brasileiro", "12345678909", java.time.LocalDate.of(1995, 7, 3), "58000-000", "Rua Exemplo", "123", "Bairro", "Cidade", "PB", br.ufpb.dcx.projetos.artista.models.StatusVerificacao.NAO_SOLICITADO, "usuario-id-1")));

        String html = engine.process("artistas/lista", contexto);

        assertTrue(html.contains("Djavan"));
        assertTrue(html.contains("Editar"));
        assertTrue(html.contains("Excluir"));
    }

    @Test
    void deveRenderizarAcaoDeEdicaoComIdDoArtista() {
        TemplateEngine engine = criarTemplateEngine();
        Context contexto = new Context();
        Artista artista = new Artista(
                "artista-123",
                "Djavan",
                "MPB",
                "Cantor e compositor brasileiro",
                "12345678909",
                java.time.LocalDate.of(1995, 7, 3),
                "58000-000",
                "Rua Exemplo",
                "123",
                "Bairro",
                "Cidade",
                "PB",
                br.ufpb.dcx.projetos.artista.models.StatusVerificacao.NAO_SOLICITADO,
                "usuario-id-1"
        );
        contexto.setVariable("form", ArtistaDTOView.edicao(artista));

        String html = engine.process("artistas/formulario", contexto);

        assertTrue(html.contains("action=\"/artistas/artista-123\""));
        assertTrue(html.contains("value=\"Djavan\""));
    }

    private TemplateEngine criarTemplateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");

        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
