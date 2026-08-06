package br.ufpb.dcx.projetos.comunidade;

import br.ufpb.dcx.projetos.comunidade.controllers.ComunidadeController;
import br.ufpb.dcx.projetos.comunidade.repositories.ComunidadeDbRepository;
import br.ufpb.dcx.projetos.comunidade.services.ComunidadeService;
import br.ufpb.dcx.projetos.infra.database.ConnectionFactory;
import io.javalin.Javalin;

public class ComunidadeModule {

    public static void registrar(Javalin app, ConnectionFactory factory, br.ufpb.dcx.projetos.login.services.UsuarioService usuarioService,
                                 br.ufpb.dcx.projetos.musica.services.MusicaService musicaService,
                                 br.ufpb.dcx.projetos.album.services.AlbumService albumService,
                                 br.ufpb.dcx.projetos.artista.services.ArtistaService artistaService) {
        var repo = new ComunidadeDbRepository(factory);
        var service = new ComunidadeService(repo);

        service.registrarObserver(new br.ufpb.dcx.projetos.comunidade.services.ComunidadeObserver() {
            private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("AuditoriaComunidade");

            @Override
            public void onPostCriado(br.ufpb.dcx.projetos.comunidade.models.Post post) {
                LOGGER.info("AUDITORIA: Novo Post criado! id={} usuarioId={}", post.getId(), post.getUsuarioId());
            }

            @Override
            public void onComentarioCriado(br.ufpb.dcx.projetos.comunidade.models.Comentario comentario) {
                LOGGER.info("AUDITORIA: Novo Comentário adicionado! id={} no post={}", comentario.getId(), comentario.getPostId());
            }
        });

        var controller = new ComunidadeController(service, usuarioService, musicaService, albumService, artistaService);
        controller.registrarRotas(app);
    }
}
