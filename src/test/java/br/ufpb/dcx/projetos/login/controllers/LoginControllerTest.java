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

class LoginControllerTest {

    private FakeUsuarioRepository repo;
    private UsuarioService service;

    @BeforeEach
    void setup() {
        repo = new FakeUsuarioRepository();
        service = new UsuarioService(repo, new PlaintextHashingStrategy());
        
        // Cadastra um usuário padrão para testes de login
        repo.salvar(new Usuario("Admin User", "admin", "admin@email.com", "senha123"));
    }

    @Test
    void deveExibirTelaDeLogin() {
        JavalinTest.test(criarApp(), (server, client) -> {
            try (var resposta = client.get("/login")) {
                assertEquals(200, resposta.code());
                assertTrue(resposta.body().string().contains("Entrar no Lynotes"));
            }
        });
    }

    @Test
    void deveAutenticarComUsernameSucessoRedirecionar() {
        JavalinTest.test(criarApp(), (server, client) -> {
            OkHttpClient http = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .build();
            FormBody body = new FormBody.Builder()
                    .add("usuario", "admin")
                    .add("senha", "senha123")
                    .build();
            Request request = new Request.Builder()
                    .url("http://localhost:" + server.port() + "/login")
                    .post(body)
                    .build();

            try (var resposta = http.newCall(request).execute()) {
                assertEquals(302, resposta.code());
                assertEquals("/", resposta.header("Location"));
            }
        });
    }

    @Test
    void deveRejeitarLoginComSenhaIncorreta() {
        JavalinTest.test(criarApp(), (server, client) -> {
            OkHttpClient http = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .build();
            FormBody body = new FormBody.Builder()
                    .add("usuario", "admin@email.com")
                    .add("senha", "senha_errada")
                    .build();
            Request request = new Request.Builder()
                    .url("http://localhost:" + server.port() + "/login")
                    .post(body)
                    .build();

            try (var resposta = http.newCall(request).execute()) {
                assertEquals(401, resposta.code());
                assertTrue(resposta.body().string().contains("Usuário ou senha inválidos."));
            }
        });
    }

    @Test
    void deveFazerLogoutRedirecionarParaHome() {
        JavalinTest.test(criarApp(), (server, client) -> {
            OkHttpClient http = new OkHttpClient.Builder()
                    .followRedirects(false)
                    .build();
            FormBody body = new FormBody.Builder().build();
            Request request = new Request.Builder()
                    .url("http://localhost:" + server.port() + "/logout")
                    .post(body)
                    .build();

            try (var resposta = http.newCall(request).execute()) {
                assertEquals(302, resposta.code());
                assertEquals("/", resposta.header("Location"));
            }
        });
    }

    private Javalin criarApp() {
        Javalin app = Javalin.create(config ->
                config.fileRenderer(new JavalinThymeleaf(templateEngine())));
        new LoginController(service).registrarRotas(app);
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
        public Optional<Usuario> buscarPorUsername(String username) {
            return usuarios.values().stream()
                    .filter(u -> u.getUsername() != null && u.getUsername().equalsIgnoreCase(username.trim()))
                    .findFirst();
        }

        @Override
        public List<Usuario> listarTodos() {
            return new ArrayList<>(usuarios.values());
        }

        @Override
        public List<Usuario> buscar(String termo) {
            if (termo == null || termo.trim().isEmpty()) {
                return listarTodos();
            }
            return usuarios.values().stream()
                    .filter(u -> (u.getNome() != null && u.getNome().toLowerCase().contains(termo.toLowerCase().trim()))
                            || (u.getEmail() != null && u.getEmail().toLowerCase().contains(termo.toLowerCase().trim()))
                            || (u.getUsername() != null && u.getUsername().toLowerCase().contains(termo.toLowerCase().trim())))
                    .toList();
        }

        @Override
        public void remover(String id) {
            usuarios.remove(id);
        }
    }
}
