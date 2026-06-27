package br.ufpb.dcx.projetos.album;

import br.ufpb.dcx.projetos.album.controllers.AlbumController;
import br.ufpb.dcx.projetos.album.repositories.AlbumDbRepository;
import br.ufpb.dcx.projetos.album.repositories.AlbumRepository;
import br.ufpb.dcx.projetos.album.repositories.AlbumSchemaInitializer;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import br.ufpb.dcx.projetos.musica.controllers.MusicaController;
import br.ufpb.dcx.projetos.musica.repositories.MusicaDbRepository;
import br.ufpb.dcx.projetos.musica.repositories.MusicaRepository;
import br.ufpb.dcx.projetos.musica.services.MusicaService;
import br.ufpb.dcx.projetos.playlist.repositories.PlaylistDbRepository;
import br.ufpb.dcx.projetos.playlist.repositories.PlaylistRepository;
import br.ufpb.dcx.projetos.playlist.services.PlaylistService;
import io.javalin.Javalin;

public final class AlbumModule {

    private AlbumModule() {
    }

    public static void registrar(Javalin app, ConnectionFactory connectionFactory, br.ufpb.dcx.projetos.login.services.UsuarioService usuarioService) {
        AlbumSchemaInitializer.inicializar(connectionFactory);
        
        AlbumRepository albumRepository = new AlbumDbRepository(connectionFactory);
        MusicaRepository musicaRepository = new MusicaDbRepository(connectionFactory);
        PlaylistRepository playlistRepository = new PlaylistDbRepository(connectionFactory);
        
        AlbumService albumService = new AlbumService(albumRepository);
        MusicaService musicaService = new MusicaService(musicaRepository, albumRepository);
        PlaylistService playlistService = new PlaylistService(playlistRepository);
        
        new AlbumController(albumService).registrarRotas(app);
        new MusicaController(musicaService, albumService, playlistService).registrarRotas(app);
        new br.ufpb.dcx.projetos.login.controllers.UsuarioController(usuarioService, musicaService).registrarRotas(app);
    }
}
