package br.ufpb.dcx.projetos;

import br.ufpb.dcx.projetos.artista.ArtistaModule;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.infra.database.DriverManagerConnectionFactory;
import br.ufpb.dcx.projetos.login.controllers.LoginController;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.util.Objects;

public final class App {

    private static final Logger LOGGER = LoggerFactory.getLogger(App.class);
    private static final int PORTA = 8080;

    private App() {
    }

    public static void main(String[] args) {
        carregarEnv();
        int porta = obterPorta();
        criarApp().start(porta);
        LOGGER.info("Servidor iniciado. porta={}", porta);
    }

    public static Javalin criarApp() {
        carregarEnv();
        Javalin app = criarJavalin();
        registrarRotasPublicas(app);

        String dbUrl = variavelAmbiente("DB_URL", "jdbc:postgresql://localhost:5432/spotify_db");
        String dbUser = variavelAmbiente("DB_USER", "postgres");
        String dbPass = variavelAmbiente("DB_PASSWORD", "postgres");

        var repo = new br.ufpb.dcx.projetos.login.repositories.UsuarioDbRepository(dbUrl, dbUser, dbPass);
        var service = new br.ufpb.dcx.projetos.login.services.UsuarioService(repo, new br.ufpb.dcx.projetos.login.services.BCryptHashingStrategy());

        registrarAutenticacao(app, service);

        ArtistaModule.registrar(app, criarConnectionFactory());
        br.ufpb.dcx.projetos.album.AlbumModule.registrar(app, criarConnectionFactory(), service);
        br.ufpb.dcx.projetos.playlist.PlaylistModule.registrar(app, criarConnectionFactory());
        registrarLogHttp(app);
        registrarTratamentoDeErrosGlobais(app);
        return app;
    }

    private static void carregarEnv() {
        try {
            java.io.File envFile = new java.io.File(".env");
            if (envFile.exists()) {
                java.nio.file.Files.lines(envFile.toPath())
                        .map(String::trim)
                        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
                        .forEach(line -> {
                            int eqIdx = line.indexOf('=');
                            if (eqIdx > 0) {
                                String key = line.substring(0, eqIdx).trim();
                                String val = line.substring(eqIdx + 1).trim();
                                System.setProperty(key, val);
                            }
                        });
            }
        } catch (Exception e) {
            // Ignorar falhas de leitura
        }
    }

    private static int obterPorta() {
        String p = variavelAmbiente("PORT", null);
        if (p != null) {
            try {
                return Integer.parseInt(p);
            } catch (NumberFormatException e) {
                // usar padrão
            }
        }
        return PORTA;
    }

    private static Javalin criarJavalin() {
        return Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.fileRenderer(new JavalinThymeleaf(criarTemplateEngine()));
        });
    }

    private static void registrarRotasPublicas(Javalin app) {
        app.get("/ping", ctx -> ctx.json(java.util.Map.of(
                "status", "ok",
                "service", "eq09",
                "timestamp", java.time.Instant.now().toString()
        )));
    }

    private static void registrarAutenticacao(Javalin app, br.ufpb.dcx.projetos.login.services.UsuarioService service) {
        LoginController loginController = new LoginController(service);
        loginController.registrarRotas(app);
        app.beforeMatched(ctx -> {
            if (rotaProtegida(ctx.path(), ctx.method().name()) && !LoginController.estaAutenticado(ctx)) {
                ctx.redirect("/login");
                ctx.skipRemainingHandlers();
            }
            if (rotaAdmin(ctx.path()) && !isAdmin(ctx)) {
                ctx.status(403).result("Acesso negado. Esta rota é restrita a administradores.");
                ctx.skipRemainingHandlers();
            }
        });
    }

    private static boolean rotaAdmin(String path) {
        return path.startsWith("/admin");
    }

    private static boolean isAdmin(io.javalin.http.Context ctx) {
        if (!LoginController.estaAutenticado(ctx)) return false;
        String tipo = ctx.sessionAttribute("usuarioLogadoTipo");
        return "ADMIN".equals(tipo);
    }

    private static boolean rotaProtegida(String path, String method) {
        // Permite o acesso público para criação de novos usuários (GET e POST)
        if (path.equals("/usuarios/novo") && method.equalsIgnoreCase("GET")) {
            return false;
        }
        if (path.equals("/usuarios") && method.equalsIgnoreCase("POST")) {
            return false;
        }
        return path.startsWith("/artistas") || path.startsWith("/usuarios") || path.startsWith("/albums") || path.startsWith("/musicas") || path.startsWith("/playlists") || path.startsWith("/api/playlists");
    }

    private static void registrarLogHttp(Javalin app) {
        app.after(ctx -> LOGGER.info(
                "Requisição HTTP concluída. metodo={} path={} status={}",
                ctx.method(),
                ctx.path(),
                ctx.statusCode()
        ));
    }

    private static void registrarTratamentoDeErrosGlobais(Javalin app) {
        app.error(404, ctx -> {
            ctx.render("erros/404");
        });

        app.error(500, ctx -> {
            ctx.render("erros/500");
        });

        app.exception(Exception.class, (e, ctx) -> {
            LOGGER.error("Erro interno não tratado pego pelo manipulador global", e);
            ctx.status(500);
            ctx.render("erros/500");
        });
    }

    private static ConnectionFactory criarConnectionFactory() {
        return new DriverManagerConnectionFactory(
                variavelAmbiente("DB_URL", "jdbc:postgresql://localhost:5432/spotify_db"),
                variavelAmbiente("DB_USER", "postgres"),
                variavelAmbiente("DB_PASSWORD", "postgres")
        );
    }

    private static String variavelAmbiente(String nome, String valorPadrao) {
        String valor = System.getProperty(nome);
        if (valor == null) {
            valor = System.getenv(nome);
        }
        return Objects.isNull(valor) || valor.isBlank() ? valorPadrao : valor;
    }

    private static TemplateEngine criarTemplateEngine() {
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
