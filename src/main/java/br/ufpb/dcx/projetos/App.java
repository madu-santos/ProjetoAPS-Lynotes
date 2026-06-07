package br.ufpb.dcx.projetos;

import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

public class App {

    public static void main(String[] args) {
        // Inicializa o Javalin e configura a renderização e arquivos estáticos
        Javalin app = criarApp();

        // Inicia o servidor na porta 8080
        int porta = 8080;
        app.start(porta);
        System.out.println("Servidor iniciado com sucesso em http://localhost:" + porta);
    }

    /**
     * Cria e configura a instância do Javalin.
     * Separamos essa lógica em um método próprio para facilitar a execução de testes automatizados
     * (o JUnit pode criar a app na memória sem precisar iniciar o servidor em uma porta física).
     */
    public static Javalin criarApp() {
        return Javalin.create(config -> {
            // Configura a pasta de arquivos estáticos (CSS, JS, imagens) localizada em src/main/resources/public
            config.staticFiles.add("/public");

            // Configura o Thymeleaf com prefixo /templates/ e sufixo .html
            config.fileRenderer(new JavalinThymeleaf(thymeleafEngine()));
        })
        // Mapeia a rota inicial (raiz) para renderizar a página index (Thymeleaf resolve para templates/index.html)
        .get("/", ctx -> ctx.render("index"));
    }

    /**
     * Configura o motor de templates Thymeleaf com prefixo, sufixo e codificação padrão.
     */
    private static TemplateEngine thymeleafEngine() {
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
