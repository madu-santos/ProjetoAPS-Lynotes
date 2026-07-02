package br.ufpb.dcx.projetos.playlist;

import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.playlist.controllers.PlaylistController;
import br.ufpb.dcx.projetos.playlist.repositories.PlaylistDbRepository;
import br.ufpb.dcx.projetos.playlist.repositories.PlaylistRepository;
import br.ufpb.dcx.projetos.playlist.services.PlaylistService;
import io.javalin.Javalin;

public final class PlaylistModule {

    private PlaylistModule() {
    }

    public static void registrar(Javalin app, ConnectionFactory connectionFactory) {
        PlaylistRepository playlistRepository = new PlaylistDbRepository(connectionFactory);
        PlaylistService playlistService = new PlaylistService(playlistRepository);
        new PlaylistController(playlistService).registrarRotas(app);
    }
}
