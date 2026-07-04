package br.ufpb.dcx.projetos;

import br.ufpb.dcx.projetos.artista.ArtistaModule;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.infra.database.DriverManagerConnectionFactory;
import br.ufpb.dcx.projetos.login.controllers.LoginController;
import br.ufpb.dcx.projetos.login.models.Role;
import io.javalin.Javalin;
import io.javalin.http.HttpStatus;
import io.javalin.rendering.template.JavalinThymeleaf;
import io.javalin.security.RouteRole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import io.javalin.openapi.OpenApi;
import io.javalin.openapi.plugin.OpenApiPlugin;
import io.javalin.openapi.plugin.swagger.SwaggerPlugin;
import org.flywaydb.core.Flyway;

import java.util.Objects;
import java.util.Set;

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

        app.beforeMatched(ctx -> {
            Set<RouteRole> routeRoles = ctx.routeRoles();
            if (routeRoles.isEmpty() || routeRoles.contains(Role.ANYONE)) {
                return;
            }
            if (!LoginController.estaAutenticado(ctx)) {
                ctx.redirect("/login");
            } else if (routeRoles.contains(Role.ADMIN)) {
                String tipo = ctx.sessionAttribute("usuarioLogadoTipo");
                if (!"ADMIN".equals(tipo)) {
                    ctx.status(403).result("Acesso Negado");
                }
            }
        });

        registrarRotasPublicas(app);

        String dbUrl = variavelAmbiente("DB_URL", "jdbc:postgresql://localhost:5432/spotify_db");
        String dbUser = variavelAmbiente("DB_USER", "postgres");
        String dbPass = variavelAmbiente("DB_PASSWORD", "postgres");

        // Executar Flyway Migrations
        try {
            LOGGER.info("Iniciando migrations com Flyway...");
            Flyway.configure()
                    .dataSource(dbUrl, dbUser, dbPass)
                    .baselineOnMigrate(true)
                    .load()
                    .migrate();
        } catch (Exception e) {
            LOGGER.warn("Erro ao executar Flyway Migrations (provavelmente banco offline): {}", e.getMessage());
        }

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
            config.registerPlugin(new OpenApiPlugin(openapi -> {
                openapi.withDefinitionConfiguration((version, definition) -> {
                    definition.withInfo(info -> {
                        info.title("Lynotes API");
                        info.version("1.0.0");
                    });
                });
            }));
            config.registerPlugin(new SwaggerPlugin(swagger -> {
                swagger.setUiPath("/swagger");
            }));
        });
    }

    @OpenApi(
        summary = "Verificar Saúde da API",
        path = "/ping",
        methods = io.javalin.openapi.HttpMethod.GET,
        tags = {"Sistema"}
    )
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
