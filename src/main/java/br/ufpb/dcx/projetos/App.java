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
        criarApp().start(PORTA);
        LOGGER.info("Servidor iniciado. porta={}", PORTA);
    }

    public static Javalin criarApp() {
        Javalin app = criarJavalin();
        registrarRotasPublicas(app);

        String dbUrl = variavelAmbiente("DB_URL", "jdbc:postgresql://localhost:5432/spotify_db");
        String dbUser = variavelAmbiente("DB_USER", "postgres");
        String dbPass = variavelAmbiente("DB_PASSWORD", "postgres");

        var repo = new br.ufpb.dcx.projetos.login.repositories.UsuarioDbRepository(dbUrl, dbUser, dbPass);
        var service = new br.ufpb.dcx.projetos.login.services.UsuarioService(repo, new br.ufpb.dcx.projetos.login.services.BCryptHashingStrategy());

        registrarAutenticacao(app, service);

        var controller = new br.ufpb.dcx.projetos.login.controllers.UsuarioController(service);
        controller.registrarRotas(app);

        ArtistaModule.registrar(app, criarConnectionFactory());
        registrarLogHttp(app);
        return app;
    }

    private static Javalin criarJavalin() {
        return Javalin.create(config -> {
            config.staticFiles.add("/public");
            config.fileRenderer(new JavalinThymeleaf(criarTemplateEngine()));
        });
    }

    private static void registrarRotasPublicas(Javalin app) {
        app.get("/", ctx -> ctx.render("index"));
    }

    private static void registrarAutenticacao(Javalin app, br.ufpb.dcx.projetos.login.services.UsuarioService service) {
        LoginController loginController = new LoginController(service);
        loginController.registrarRotas(app);
        app.beforeMatched(ctx -> {
            if (rotaProtegida(ctx.path(), ctx.method().name()) && !LoginController.estaAutenticado(ctx)) {
                ctx.redirect("/login");
                ctx.skipRemainingHandlers();
            }
        });
    }

    private static boolean rotaProtegida(String path, String method) {
        // Permite o acesso público para criação de novos usuários (GET e POST)
        if (path.equals("/usuarios/novo") && method.equalsIgnoreCase("GET")) {
            return false;
        }
        if (path.equals("/usuarios") && method.equalsIgnoreCase("POST")) {
            return false;
        }
        return path.startsWith("/artistas") || path.startsWith("/usuarios");
    }

    private static void registrarLogHttp(Javalin app) {
        app.after(ctx -> LOGGER.info(
                "Requisição HTTP concluída. metodo={} path={} status={}",
                ctx.method(),
                ctx.path(),
                ctx.statusCode()
        ));
    }

    private static ConnectionFactory criarConnectionFactory() {
        return new DriverManagerConnectionFactory(
                variavelAmbiente("DB_URL", "jdbc:postgresql://localhost:5432/spotify_db"),
                variavelAmbiente("DB_USER", "postgres"),
                variavelAmbiente("DB_PASSWORD", "postgres")
        );
    }

    private static String variavelAmbiente(String nome, String valorPadrao) {
        String valor = System.getenv(nome);
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
