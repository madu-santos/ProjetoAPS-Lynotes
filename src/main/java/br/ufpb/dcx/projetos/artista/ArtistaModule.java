package br.ufpb.dcx.projetos.artista;

import br.ufpb.dcx.projetos.artista.controllers.AdminController;
import br.ufpb.dcx.projetos.artista.controllers.ArtistaController;
import br.ufpb.dcx.projetos.artista.repositories.ArtistaDbRepository;
import br.ufpb.dcx.projetos.artista.repositories.ArtistaRepository;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import br.ufpb.dcx.projetos.artista.services.ArtistaUseCase;
import br.ufpb.dcx.projetos.artista.services.ArtistaValidator;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.login.models.Role;
import io.javalin.Javalin;

public final class ArtistaModule {

    private ArtistaModule() {
    }

    public static void registrar(Javalin app, ConnectionFactory connectionFactory, br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService) {
        ArtistaRepository repository = new ArtistaDbRepository(connectionFactory);
        ArtistaUseCase useCase = new ArtistaService(repository, new ArtistaValidator());

        ArtistaController controller = new ArtistaController(useCase, comunidadeService);
        AdminController adminController = new AdminController((ArtistaService) useCase);

        // Registro de rotas de Artistas
        app.get("/artistas", controller::listar, Role.USER);
        app.get("/artistas/novo", controller::exibirCadastro, Role.USER);
        app.post("/artistas", controller::cadastrar, Role.USER);
        app.get("/artistas/{id}/editar", controller::exibirEdicao, Role.USER);
        app.post("/artistas/{id}", controller::atualizar, Role.USER);
        app.post("/artistas/{id}/excluir", controller::excluir, Role.USER);

        // Registro de rotas de Admin
        app.get("/admin/verificacoes", adminController::listarPendentes, Role.ADMIN);
        app.post("/admin/verificacoes/{id}/aprovar", adminController::aprovar, Role.ADMIN);
        app.post("/admin/verificacoes/{id}/rejeitar", adminController::rejeitar, Role.ADMIN);
    }
}
