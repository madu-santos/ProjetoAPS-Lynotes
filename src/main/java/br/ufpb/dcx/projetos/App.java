package br.ufpb.dcx.projetos;

import br.ufpb.dcx.projetos.artista.ArtistaModule;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.infra.database.DriverManagerConnectionFactory;
import br.ufpb.dcx.projetos.login.controllers.LoginController;
import br.ufpb.dcx.projetos.login.models.Role;
import br.ufpb.dcx.projetos.musica.MusicaModule;
import br.ufpb.dcx.projetos.exceptions.GlobalHttpExceptionHandler;
import io.javalin.Javalin;
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
import java.util.stream.Stream;

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

        validarConfiguracaoBanco();

        // Access Manager replacement for Javalin 6
        app.beforeMatched(ctx -> {
            Set<RouteRole> routeRoles = ctx.routeRoles();
            
            if (routeRoles.isEmpty() || routeRoles.contains(Role.ANYONE)) {
                return;
            }

            if (!LoginController.estaAutenticado(ctx)) {
                throw new io.javalin.http.RedirectResponse(io.javalin.http.HttpStatus.FOUND, "/login");
            }

            if (routeRoles.contains(Role.ADMIN)) {
                String tipo = ctx.sessionAttribute("usuarioLogadoTipo");
                if (!"ADMIN".equals(tipo)) {
                    throw new io.javalin.http.ForbiddenResponse("Acesso Negado");
                }
            }
        });

        registrarRotasPublicas(app);

        String dbUrl = variavelAmbiente("DB_URL", null);
        String dbUser = variavelAmbiente("DB_USER", null);
        String dbPass = variavelAmbiente("DB_PASSWORD", null);

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

        var dbRepo = new br.ufpb.dcx.projetos.login.repositories.UsuarioDbRepository(dbUrl, dbUser, dbPass);
        var repo = new br.ufpb.dcx.projetos.login.repositories.LoggingUsuarioRepositoryDecorator(dbRepo);
        var hashingAlgorithm = variavelAmbiente("HASH_ALGORITHM", "BCRYPT");
        var hashingStrategy = br.ufpb.dcx.projetos.login.services.HashingStrategyFactory.criar(hashingAlgorithm);
        var service = new br.ufpb.dcx.projetos.login.services.UsuarioService(repo, hashingStrategy);

        registrarAutenticacao(app, service);

        var connectionFactory = criarConnectionFactory();
        
        // Garante que o esquema do banco (incluindo colunas e tabelas) esteja atualizado
        try {
            br.ufpb.dcx.projetos.album.repositories.AlbumSchemaInitializer.inicializar(connectionFactory);
        } catch (Exception e) {
            LOGGER.warn("Aviso ao inicializar esquema do álbum: {}", e.getMessage());
        }

        var comunidadeRepo = new br.ufpb.dcx.projetos.comunidade.repositories.ComunidadeDbRepository(connectionFactory);
        var comunidadeService = new br.ufpb.dcx.projetos.comunidade.services.ComunidadeService(comunidadeRepo);

        // Interceptor para Bolinha Vermelha de Notificação da Comunidade
        app.before(ctx -> {
            try {
                String logadoId = ctx.sessionAttribute("usuarioLogadoId");
                if (logadoId != null && !ctx.path().startsWith("/comunidade") && !ctx.path().startsWith("/public") && !ctx.path().startsWith("/js") && !ctx.path().startsWith("/css")) {
                    java.time.LocalDateTime lastVisited = ctx.sessionAttribute("comunidadeLastVisited");
                    if (lastVisited == null) {
                        lastVisited = java.time.LocalDateTime.now().minusDays(1);
                    }
                    boolean temNovos = comunidadeService.temNovosPosts(logadoId, lastVisited);
                    boolean temPedidos = !comunidadeService.listarPedidosRecebidos(logadoId).isEmpty();
                    ctx.sessionAttribute("novidadesComunidade", temNovos || temPedidos);
                }
            } catch (Exception ignored) {
                // Interceptor silencioso para nunca causar Erro 500
            }
        });

        // Inicializar repositórios e serviços de música e álbum antes dos módulos que dependem deles
        var musicaRepo = new br.ufpb.dcx.projetos.musica.repositories.LoggingMusicaRepositoryDecorator(new br.ufpb.dcx.projetos.musica.repositories.MusicaDbRepository(connectionFactory));
        var albumRepo = new br.ufpb.dcx.projetos.album.repositories.AlbumDbRepository(connectionFactory);
        var musicaService = new br.ufpb.dcx.projetos.musica.services.MusicaService(musicaRepo, albumRepo);
        var artistaRepo = new br.ufpb.dcx.projetos.artista.repositories.ArtistaDbRepository(connectionFactory);
        var artistaService = new br.ufpb.dcx.projetos.artista.services.ArtistaService(artistaRepo, new br.ufpb.dcx.projetos.artista.services.ArtistaValidator());
        var albumService = new br.ufpb.dcx.projetos.album.services.AlbumService(albumRepo);
        
        var playlistRepo = new br.ufpb.dcx.projetos.playlist.repositories.LoggingPlaylistRepositoryDecorator(new br.ufpb.dcx.projetos.playlist.repositories.PlaylistDbRepository(connectionFactory));
        var playlistService = new br.ufpb.dcx.projetos.playlist.services.PlaylistService(playlistRepo);

        ArtistaModule.registrar(app, connectionFactory, comunidadeService);
        br.ufpb.dcx.projetos.album.AlbumModule.registrar(app, connectionFactory, comunidadeService, musicaService);
        MusicaModule.registrar(app, connectionFactory, comunidadeService);
        br.ufpb.dcx.projetos.playlist.PlaylistModule.registrar(app, connectionFactory, musicaService, comunidadeService);
        br.ufpb.dcx.projetos.comunidade.ComunidadeModule.registrar(app, connectionFactory, service, musicaService, albumService, artistaService);

        var rankingController = new br.ufpb.dcx.projetos.rankings.RankingController(musicaService, albumService, artistaService);
        app.get("/rankings", rankingController::index, Role.USER);
        
        var youtubeService = new br.ufpb.dcx.projetos.infra.youtube.YouTubeService();
        var youtubeController = new br.ufpb.dcx.projetos.infra.youtube.controllers.YouTubeController(youtubeService);
        app.get("/api/youtube/search", youtubeController::buscar, Role.USER);
        app.get("/api/youtube/album-faixas", youtubeController::buscarFaixasPreview, Role.USER);

        new br.ufpb.dcx.projetos.login.controllers.UsuarioController(service, musicaService, artistaService, albumService, comunidadeService, playlistService).registrarRotas(app);

        registrarLogHttp(app);
        GlobalHttpExceptionHandler.registrar(app);
        registrarTratamentoDeErrosStatus(app);
        return app;
    }

    private static void validarConfiguracaoBanco() {
        if (estaEmAmbienteDeTeste()) {
            return;
        }

        String url = variavelAmbiente("DB_URL", null);
        String user = variavelAmbiente("DB_USER", null);
        String pass = variavelAmbiente("DB_PASSWORD", null);

        if (url == null || user == null || pass == null) {
            throw new IllegalStateException(
                    "Configuração de banco de dados ausente. As variáveis de ambiente " +
                    "DB_URL, DB_USER e DB_PASSWORD são obrigatórias em produção."
            );
        }
    }

    private static boolean estaEmAmbienteDeTeste() {
        // Detecta se está rodando via JUnit ou se a variável de ambiente APP_ENV é 'test'
        String env = variavelAmbiente("APP_ENV", "prod");
        if ("test".equalsIgnoreCase(env)) return true;
        
        return Stream.of(Thread.currentThread().getStackTrace())
                .anyMatch(s -> s.getClassName().contains("org.junit") || s.getClassName().contains("JavalinTest"));
    }

    private static void carregarEnv() {
        try {
            java.io.File envFile = new java.io.File(".env");
            if (envFile.exists()) {
                try (java.util.stream.Stream<String> lines = java.nio.file.Files.lines(envFile.toPath())) {
                    lines.map(String::trim)
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
            config.registerPlugin(new OpenApiPlugin(openapi -> openapi.withDefinitionConfiguration((version, definition) ->
                    definition.withInfo(info -> {
                        info.title("Lynotes API");
                        info.version("1.0.0");
                    }))));
            config.registerPlugin(new SwaggerPlugin(swagger -> swagger.setUiPath("/swagger")));
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

    private static void registrarTratamentoDeErrosStatus(Javalin app) {
        app.error(404, ctx -> ctx.render("erros/404"));

        app.error(500, ctx -> ctx.render("erros/500"));
    }

    private static ConnectionFactory criarConnectionFactory() {
        return new DriverManagerConnectionFactory(
                variavelAmbiente("DB_URL", null),
                variavelAmbiente("DB_USER", null),
                variavelAmbiente("DB_PASSWORD", null)
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
