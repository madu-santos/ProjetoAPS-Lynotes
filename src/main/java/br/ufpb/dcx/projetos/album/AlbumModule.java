package br.ufpb.dcx.projetos.album;

import br.ufpb.dcx.projetos.album.controllers.AlbumController;
import br.ufpb.dcx.projetos.album.repositories.AlbumDbRepository;
import br.ufpb.dcx.projetos.album.repositories.AlbumRepository;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.login.models.Role;
import io.javalin.Javalin;

public final class AlbumModule {

    private AlbumModule() {
    }

    public static void registrar(Javalin app, ConnectionFactory connectionFactory, br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService, br.ufpb.dcx.projetos.musica.services.MusicaService musicaService) {
        AlbumRepository albumRepository = new AlbumDbRepository(connectionFactory);
        AlbumService albumService = new AlbumService(albumRepository);
        AlbumController controller = new AlbumController(albumService, comunidadeService, musicaService);

        app.get("/albums", controller::listar, Role.USER);
        app.get("/albuns", controller::listar, Role.USER);
        app.get("/albums/novo", controller::exibirCadastro, Role.USER);
        app.get("/albuns/novo", controller::exibirCadastro, Role.USER);
        app.get("/albums/{id}", controller::exibirDetalhes, Role.USER);
        app.get("/albuns/{id}", controller::exibirDetalhes, Role.USER);
        app.post("/albums", controller::cadastrar, Role.USER);
        app.post("/albuns", controller::cadastrar, Role.USER);
        app.get("/albums/{id}/editar", controller::exibirEdicao, Role.USER);
        app.get("/albuns/{id}/editar", controller::exibirEdicao, Role.USER);
        app.post("/albums/{id}", controller::atualizar, Role.USER);
        app.post("/albuns/{id}", controller::atualizar, Role.USER);
        app.post("/albums/{id}/excluir", controller::excluir, Role.USER);
        app.post("/albuns/{id}/excluir", controller::excluir, Role.USER);
        app.get("/api/albums/{id}/faixas", controller::listarFaixasApi);
        app.get("/api/albuns/{id}/faixas", controller::listarFaixasApi);
        app.post("/albums/{albumId}/faixas/{faixaId}/toggle-oculta", controller::alternarOcultaFaixa, Role.USER);
        app.post("/albuns/{albumId}/faixas/{faixaId}/toggle-oculta", controller::alternarOcultaFaixa, Role.USER);
    }
}
