package br.ufpb.dcx.projetos.musica;

import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.musica.controllers.MusicaController;
import br.ufpb.dcx.projetos.musica.repositories.MusicaDbRepository;
import br.ufpb.dcx.projetos.musica.services.MusicaService;
import br.ufpb.dcx.projetos.playlist.services.PlaylistService;
import br.ufpb.dcx.projetos.album.repositories.AlbumDbRepository;
import br.ufpb.dcx.projetos.playlist.repositories.PlaylistDbRepository;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import br.ufpb.dcx.projetos.artista.repositories.ArtistaDbRepository;
import br.ufpb.dcx.projetos.artista.services.ArtistaValidator;
import br.ufpb.dcx.projetos.login.models.Role;
import io.javalin.Javalin;

public final class MusicaModule {

    private MusicaModule() {
    }

    public static void registrar(Javalin app, ConnectionFactory connectionFactory, br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService) {
        var albumRepository = new AlbumDbRepository(connectionFactory);
        var musicaRepository = new br.ufpb.dcx.projetos.musica.repositories.LoggingMusicaRepositoryDecorator(new MusicaDbRepository(connectionFactory));
        var playlistRepository = new PlaylistDbRepository(connectionFactory);
        var artistaRepository = new ArtistaDbRepository(connectionFactory);

        var albumService = new AlbumService(albumRepository);
        var musicaService = new MusicaService(musicaRepository, albumRepository);
        var playlistService = new PlaylistService(playlistRepository);
        var artistaService = new ArtistaService(artistaRepository, new ArtistaValidator());

        var controller = new MusicaController(musicaService, albumService, playlistService, artistaService, comunidadeService);
        
        app.get("/", controller::exibirHomeOuLanding, Role.ANYONE);
        app.get("/musicas", controller::listar, Role.USER);
        app.get("/musicas/todas", controller::listarTodas, Role.USER);
        app.get("/musicas/salvas", controller::listarTodas, Role.USER);
        app.get("/musicas/novo", controller::exibirCadastro, Role.USER);
        app.get("/musicas/new", controller::exibirCadastro, Role.USER);
        app.post("/musicas", controller::cadastrar, Role.USER);
        app.get("/musicas/edit/{id}", controller::exibirEdicao, Role.USER);
        app.get("/musicas/{id}/editar", controller::exibirEdicao, Role.USER);
        app.post("/musicas/edit/{id}", controller::atualizar, Role.USER);
        app.post("/musicas/{id}", controller::atualizar, Role.USER);
        app.post("/musicas/{id}/delete", controller::excluir, Role.USER);
        app.post("/musicas/{id}/excluir", controller::excluir, Role.USER);
        app.post("/api/musicas/{id}/youtube-id", controller::atualizarYoutubeId, Role.USER);
    }
}
