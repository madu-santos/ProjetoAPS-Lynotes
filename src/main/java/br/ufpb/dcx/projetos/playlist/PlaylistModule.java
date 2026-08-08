package br.ufpb.dcx.projetos.playlist;

import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.playlist.controllers.PlaylistController;
import br.ufpb.dcx.projetos.playlist.repositories.PlaylistDbRepository;
import br.ufpb.dcx.projetos.playlist.repositories.LoggingPlaylistRepositoryDecorator;
import br.ufpb.dcx.projetos.playlist.repositories.PlaylistRepository;
import br.ufpb.dcx.projetos.playlist.services.PlaylistService;
import br.ufpb.dcx.projetos.login.models.Role;
import io.javalin.Javalin;

public final class PlaylistModule {

    private PlaylistModule() {
    }

    public static void registrar(Javalin app, ConnectionFactory connectionFactory, br.ufpb.dcx.projetos.musica.services.MusicaService musicaService, br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService) {
        PlaylistRepository playlistRepository = new LoggingPlaylistRepositoryDecorator(new PlaylistDbRepository(connectionFactory));
        PlaylistService playlistService = new PlaylistService(playlistRepository);
        PlaylistController controller = new PlaylistController(playlistService, musicaService, comunidadeService);
        
        app.get("/playlists", controller::listar, Role.USER);
        app.get("/playlists/new", controller::exibirFormularioCriacao, Role.USER);
        app.get("/playlists/nova", controller::exibirFormularioCriacao, Role.USER);
        app.get("/playlists/{id}/editar", controller::exibirFormularioEdicao, Role.USER);
        app.get("/playlists/edit/{id}", controller::exibirFormularioEdicao, Role.USER);
        app.post("/playlists", controller::criarPlaylist, Role.USER);
        app.get("/playlists/{id}", controller::exibirPlaylist, Role.USER);
        app.post("/playlists/{id}/items", controller::adicionarItem, Role.USER);
        app.post("/playlists/{id}/items/{itemId}/remover", controller::removerItem, Role.USER);
        app.post("/playlists/{id}/items/{itemId}/toggle-oculta", controller::alternarOcultaItem, Role.USER);
        app.post("/playlists/{id}/editar", controller::editarPlaylist, Role.USER);
        app.post("/playlists/{id}/excluir", controller::excluirPlaylist, Role.USER);
        app.get("/api/playlists", controller::listarJson, Role.USER);
        app.post("/api/playlists", controller::criarPlaylistJson, Role.USER);
        app.get("/api/playlists/{id}", controller::obterPlaylistJson, Role.USER);
        app.get("/api/playlists/{id}/check-duplicate", controller::checkDuplicateJson, Role.USER);
        app.post("/api/playlists/{id}/items", controller::adicionarItemJson, Role.USER);
    }
}
