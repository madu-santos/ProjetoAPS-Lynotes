package br.ufpb.dcx.projetos.login.controllers;

import br.ufpb.dcx.projetos.login.models.Usuario;
import br.ufpb.dcx.projetos.login.services.UsuarioService;
import br.ufpb.dcx.projetos.login.services.PlaintextHashingStrategy;
import br.ufpb.dcx.projetos.login.repositories.UsuarioRepository;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import io.javalin.testtools.JavalinTest;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioControllerTest {

    private FakeUsuarioRepository repo;
    private UsuarioService service;

    @BeforeEach
    void setup() {
        repo = new FakeUsuarioRepository();
        service = new UsuarioService(repo, new PlaintextHashingStrategy());
    }

    @Test
    void deveExibirFormularioDeCadastro() {
        JavalinTest.test(criarApp(), (server, client) -> {
            try (var resposta = client.get("/usuarios/novo")) {
                assertEquals(200, resposta.code());
                assertTrue(resposta.body().string().contains("Criar Conta"));
            }
        });
    }

    @Test
    void deveExibirListagemDeUsuarios() {
        repo.salvar(new Usuario("Admin", "admin@email.com", "senha123"));

        JavalinTest.test(criarApp(), (server, client) -> {
            try (var resposta = client.get("/usuarios")) {
                assertEquals(200, resposta.code());
                String html = resposta.body().string();
                assertTrue(html.contains("Usuários"));
                assertTrue(html.contains("admin@email.com"));
            }
        });
    }

    @Test
    void deveRedirecionarParaLoginAposCadastroPorConvidado() {
        JavalinTest.test(criarApp(), (server, client) -> {
            OkHttpClient http = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .build();
            FormBody body = new FormBody.Builder()
                    .add("nome", "Maria Eduarda")
                    .add("email", "madu@email.com")
                    .add("senha", "senha123")
                    .build();
            Request request = new Request.Builder()
                    .url("http://localhost:" + server.port() + "/usuarios")
                    .post(body)
                    .build();

            try (var resposta = http.newCall(request).execute()) {
                assertEquals(302, resposta.code()); // Javalin default redirect status code is 302
                assertEquals("/login", resposta.header("Location"));
            }
        });
    }

    @Test
    void deveRetornar422ParaCadastroInvalido() {
        JavalinTest.test(criarApp(), (server, client) -> {
            OkHttpClient http = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .build();
            FormBody body = new FormBody.Builder()
                    .add("nome", "")
                    .add("email", "madu@email.com")
                    .add("senha", "123")
                    .build();
            Request request = new Request.Builder()
                    .url("http://localhost:" + server.port() + "/usuarios")
                    .post(body)
                    .build();

            try (var resposta = http.newCall(request).execute()) {
                assertEquals(200, resposta.code());
                assertTrue(resposta.body().string().contains("Nome é obrigatório"));
            }
        });
    }

    @Test
    void deveExibirPerfilPublicoDoUsuario() {
        Usuario usuario = new Usuario("Maria", "maria@email.com", "senha123");
        repo.salvar(usuario);

        br.ufpb.dcx.projetos.musica.repositories.MusicaRepository mockMusicaRepo = new br.ufpb.dcx.projetos.musica.repositories.MusicaRepository() {
            @Override public void criar(br.ufpb.dcx.projetos.musica.models.Musica m) {}
            @Override public boolean atualizar(br.ufpb.dcx.projetos.musica.models.Musica m) { return false; }
            @Override public Optional<br.ufpb.dcx.projetos.musica.models.Musica> buscarPorId(String id) { return Optional.empty(); }
            @Override public List<br.ufpb.dcx.projetos.musica.models.Musica> listarTodas() { return List.of(); }
            @Override public List<br.ufpb.dcx.projetos.musica.models.Musica> buscarPorAlbumId(String albumId) { return List.of(); }
            @Override public List<br.ufpb.dcx.projetos.musica.models.Musica> buscarPorUsuarioId(String usuarioId) { return List.of(); }
        };
        br.ufpb.dcx.projetos.musica.services.MusicaService musicaService = new br.ufpb.dcx.projetos.musica.services.MusicaService(mockMusicaRepo, null);

        Javalin app = Javalin.create(config ->
                config.fileRenderer(new JavalinThymeleaf(templateEngine())));
        new UsuarioController(service, musicaService).registrarRotas(app);

        JavalinTest.test(app, (server, client) -> {
            try (var resposta = client.get("/usuarios/" + usuario.getId())) {
                assertEquals(200, resposta.code());
                String html = resposta.body().string();
                assertTrue(html.contains("Catálogo de Maria"));
                assertTrue(html.contains("Este catálogo está vazio"));
            }
        });
    }

    private Javalin criarApp() {
        Javalin app = Javalin.create(config ->
                config.fileRenderer(new JavalinThymeleaf(templateEngine())));
        new UsuarioController(service, new br.ufpb.dcx.projetos.musica.services.MusicaService(null, null)).registrarRotas(app);
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

    private static class FakeUsuarioRepository implements UsuarioRepository {
        private final Map<String, Usuario> usuarios = new HashMap<>();

        @Override
        public void salvar(Usuario usuario) {
            usuarios.put(usuario.getId(), usuario);
        }

        @Override
        public Optional<Usuario> buscarPorId(String id) {
            return Optional.ofNullable(usuarios.get(id));
        }

        @Override
        public Optional<Usuario> buscarPorEmail(String email) {
            return usuarios.values().stream()
                    .filter(u -> u.getEmail().equalsIgnoreCase(email.trim()))
                    .findFirst();
        }

        @Override
        public List<Usuario> listarTodos() {
            return new ArrayList<>(usuarios.values());
        }

        @Override
    }
}
