package br.ufpb.dcx.projetos.comunidade;

import br.ufpb.dcx.projetos.comunidade.controllers.ComunidadeController;
import br.ufpb.dcx.projetos.comunidade.repositories.ComunidadeDbRepository;
import br.ufpb.dcx.projetos.comunidade.services.ComunidadeService;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import io.javalin.Javalin;

public class ComunidadeModule {

    public static void registrar(Javalin app, ConnectionFactory factory, br.ufpb.dcx.projetos.login.services.UsuarioService usuarioService) {
        var repo = new ComunidadeDbRepository(factory);
        var service = new ComunidadeService(repo);
        var controller = new ComunidadeController(service, usuarioService);
        controller.registrarRotas(app);
    }
}
