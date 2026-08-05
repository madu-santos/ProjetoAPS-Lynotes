package br.ufpb.dcx.projetos.artista.controllers;

import br.ufpb.dcx.projetos.artista.dto.ArtistaDTO;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaIdInvalidoException;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaNaoEncontradoException;
import br.ufpb.dcx.projetos.artista.exceptions.ArtistaValidacaoException;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaUseCase;
import br.ufpb.dcx.projetos.infra.database.DatabaseException;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import io.javalin.testtools.JavalinTest;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ArtistaControllerTest {

    @Test
    void deveRetornar400ParaIdInvalido() {
        JavalinTest.test(criarApp(new FakeUseCase()), (server, client) -> {
            try (var resposta = client.get("/artistas/id-invalido/editar")) {
                assertEquals(400, resposta.code());
            }
        });
    }

    @Test
    void deveRetornar404ParaArtistaInexistente() {
        FakeUseCase useCase = new FakeUseCase();
        useCase.artistaAusente = true;

        JavalinTest.test(criarApp(useCase), (server, client) -> {
            try (var resposta = client.get(
                    "/artistas/fce7e912-82d2-402f-bc19-4dba7b28361a/editar"
            )) {
                assertEquals(404, resposta.code());
            }
        });
    }

    @Test
    void deveRetornar422EManterFormularioInvalido() {
        FakeUseCase useCase = new FakeUseCase();
        useCase.formularioInvalido = true;

        JavalinTest.test(criarApp(useCase), (server, client) -> {
            OkHttpClient http = new OkHttpClient();
            FormBody body = new FormBody.Builder()
                    .add("nome", "")
                    .add("generoMusical", "Rock")
                    .add("biografia", "")
                    .build();
            Request request = new Request.Builder()
                    .url("http://localhost:" + server.port() + "/artistas")
                    .post(body)
                    .build();

            try (var resposta = http.newCall(request).execute()) {
                assertEquals(200, resposta.code());
                assertTrue(resposta.body().string().contains("Nome obrigatório"));
            }
        });
    }

    @Test
    void deveRetornar303AposCadastro() {
        JavalinTest.test(criarApp(new FakeUseCase()), (server, client) -> {
            OkHttpClient http = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .build();
            FormBody body = new FormBody.Builder()
                    .add("nome", "Djavan")
                    .add("generoMusical", "MPB")
                    .add("biografia", "")
                    .build();
            Request request = new Request.Builder()
                    .url("http://localhost:" + server.port() + "/artistas")
                    .post(body)
                    .build();

            try (var resposta = http.newCall(request).execute()) {
                assertEquals(303, resposta.code());
                assertEquals("/artistas", resposta.header("Location"));
            }
        });
    }

    @Test
    void deveRetornar500SemExporDetalhesDaPersistencia() {
        FakeUseCase useCase = new FakeUseCase();
        useCase.falhaPersistencia = true;

        JavalinTest.test(criarApp(useCase), (server, client) -> {
            try (var resposta = client.get("/artistas")) {
                String corpo = resposta.body().string();

                assertEquals(500, resposta.code());
                assertTrue(corpo.contains("Não foi possível concluir a operação"));
                assertTrue(!corpo.contains("conexão recusada"));
            }
        });
    }

    private Javalin criarApp(ArtistaUseCase useCase) {
        Javalin app = Javalin.create(config ->
                config.fileRenderer(new JavalinThymeleaf(templateEngine())));
        br.ufpb.dcx.projetos.exceptions.GlobalHttpExceptionHandler.registrar(app);
        ArtistaController controller = new ArtistaController(useCase, org.mockito.Mockito.mock(br.ufpb.dcx.projetos.comunidade.services.ComunidadeService.class));
        app.get("/artistas", controller::listar);
        app.get("/artistas/novo", controller::exibirCadastro);
        app.post("/artistas", controller::cadastrar);
        app.get("/artistas/{id}/editar", controller::exibirEdicao);
        app.post("/artistas/{id}", controller::atualizar);
        app.post("/artistas/{id}/excluir", controller::excluir);
        return app;
    }

    private TemplateEngine templateEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("/templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode("HTML");
        resolver.setCharacterEncoding("UTF-8");
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static class FakeUseCase implements ArtistaUseCase {
        boolean artistaAusente = false;
        boolean formularioInvalido = false;
        boolean falhaPersistencia = false;

        @Override
        public List<Artista> listar(String busca, String usuarioId) {
            if (falhaPersistencia) {
                throw new DatabaseException(
                        "conexão recusada",
                        new IllegalStateException("banco indisponível")
                );
            }
            return List.of();
        }

        @Override
        public Artista obter(String id, String usuarioId) {
            if (!id.matches("[0-9a-f-]{36}")) {
                throw new ArtistaIdInvalidoException(id);
            }
            if (artistaAusente) {
                throw new ArtistaNaoEncontradoException(id);
            }
            return new Artista(id, "Djavan", "MPB", null, "12345678909", java.time.LocalDate.of(1995, 7, 3), "12345-678", "Rua Exemplo", "123", "Bairro", "Cidade", "SP", br.ufpb.dcx.projetos.artista.models.StatusVerificacao.NAO_SOLICITADO, usuarioId);
        }

        @Override
        public Artista cadastrar(ArtistaDTO formulario, String usuarioId) {
            if (formularioInvalido) {
                throw new ArtistaValidacaoException("Nome obrigatório");
            }
            return Artista.novo(
                    formulario.nome(),
                    formulario.generoMusical(),
                    formulario.biografia(),
                    formulario.cpf(),
                    formulario.dataNascimento(),
                    formulario.cep(),
                    formulario.logradouro(),
                    formulario.numero(),
                    formulario.bairro(),
                    formulario.cidade(),
                    formulario.uf(),
                    Boolean.TRUE.equals(formulario.solicitarVerificacao()) ? br.ufpb.dcx.projetos.artista.models.StatusVerificacao.PENDENTE : br.ufpb.dcx.projetos.artista.models.StatusVerificacao.NAO_SOLICITADO,
                    usuarioId
            );
        }

        @Override
        public Artista atualizar(String id, ArtistaDTO formulario, String usuarioId) {
            return new Artista(
                    id,
                    formulario.nome(),
                    formulario.generoMusical(),
                    formulario.biografia(),
                    formulario.cpf(),
                    formulario.dataNascimento(),
                    formulario.cep(),
                    formulario.logradouro(),
                    formulario.numero(),
                    formulario.bairro(),
                    formulario.cidade(),
                    formulario.uf(),
                    Boolean.TRUE.equals(formulario.solicitarVerificacao()) ? br.ufpb.dcx.projetos.artista.models.StatusVerificacao.PENDENTE : br.ufpb.dcx.projetos.artista.models.StatusVerificacao.NAO_SOLICITADO,
                    usuarioId
            );
        }

        @Override
        public void remover(String id, String usuarioId) {
        }
    }
}
