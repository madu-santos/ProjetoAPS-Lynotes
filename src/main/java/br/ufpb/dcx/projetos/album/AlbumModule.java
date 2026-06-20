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
import io.javalin.Javalin;

public final class AlbumModule {

    private AlbumModule() {
    }

    public static void registrar(Javalin app, ConnectionFactory connectionFactory, br.ufpb.dcx.projetos.login.services.UsuarioService usuarioService) {
        AlbumSchemaInitializer.inicializar(connectionFactory);
        
        AlbumRepository albumRepository = new AlbumDbRepository(connectionFactory);
        MusicaRepository musicaRepository = new MusicaDbRepository(connectionFactory);
        
        AlbumService albumService = new AlbumService(albumRepository);
        MusicaService musicaService = new MusicaService(musicaRepository, albumRepository);
        
        new AlbumController(albumService).registrarRotas(app);
        new MusicaController(musicaService, albumService).registrarRotas(app);
        new br.ufpb.dcx.projetos.login.controllers.UsuarioController(usuarioService, musicaService).registrarRotas(app);
    }
}
