package br.ufpb.dcx.projetos.artista;

import br.ufpb.dcx.projetos.artista.controllers.AdminController;
import br.ufpb.dcx.projetos.artista.controllers.ArtistaController;
import br.ufpb.dcx.projetos.artista.controllers.ArtistaHttpExceptionHandler;
import br.ufpb.dcx.projetos.artista.repositories.ArtistaDbRepository;
import br.ufpb.dcx.projetos.artista.repositories.ArtistaRepository;
import br.ufpb.dcx.projetos.artista.repositories.ArtistaSchemaInitializer;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import br.ufpb.dcx.projetos.artista.services.ArtistaUseCase;
import br.ufpb.dcx.projetos.artista.services.ArtistaValidator;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import io.javalin.Javalin;

public final class ArtistaModule {

    private ArtistaModule() {
    }

    public static void registrar(Javalin app, ConnectionFactory connectionFactory) {
        ArtistaSchemaInitializer.inicializar(connectionFactory);
        ArtistaRepository repository = new ArtistaDbRepository(connectionFactory);
        ArtistaUseCase useCase = new ArtistaService(repository, new ArtistaValidator());

        ArtistaHttpExceptionHandler.registrar(app);
        ArtistaController controller = new ArtistaController(useCase);
        AdminController adminController = new AdminController((ArtistaService) useCase);

        controller.registrarRotas(app);
        adminController.registrarRotas(app);
    }
}
