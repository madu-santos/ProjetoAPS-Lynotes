package br.ufpb.dcx.projetos.musica.controllers;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.musica.dto.MusicaForm;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;
import br.ufpb.dcx.projetos.musica.views.MusicaFormView;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MusicaController {

    public static final String ROTA_LISTAGEM = "/musicas";
    public static final String ROTA_NOVO = "/musicas/new";
    public static final String ROTA_EDICAO = "/musicas/edit/{id}";
    public static final String ROTA_ATUALIZACAO = "/musicas/edit/{id}";
    public static final String ROTA_EXCLUSAO = "/musicas/delete/{id}";

    private static final Logger LOGGER = LoggerFactory.getLogger(MusicaController.class);

    private final MusicaService service;
    private final AlbumService albumService;

    public MusicaController(MusicaService service, AlbumService albumService) {
        this.service = service;
        this.albumService = albumService;
    }

    public void registrarRotas(Javalin app) {
        app.get("/", this::exibirHomeOuLanding);
        app.get(ROTA_LISTAGEM, this::listar);
        app.get(ROTA_NOVO, this::exibirCadastro);
        app.post(ROTA_LISTAGEM, this::cadastrar);
        app.get(ROTA_EDICAO, this::exibirEdicao);
        app.post(ROTA_ATUALIZACAO, this::atualizar);
        app.post(ROTA_EXCLUSAO, this::excluir);
    }

    private void exibirHomeOuLanding(Context ctx) {
        String usuarioLogado = ctx.sessionAttribute("usuarioLogado");
        if (usuarioLogado != null) {
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            List<Musica> musicas = service.findByUsuarioId(usuarioId);
            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", usuarioLogado);
            modelo.put("musicas", musicas);
            modelo.put("semMusicas", musicas.isEmpty());
            ctx.render("home", modelo);
        } else {
            ctx.render("index", Map.of());
        }
    }

    private void listar(Context ctx) {
        ctx.redirect("/");
    }

    private void exibirCadastro(Context ctx) {
        List<Album> albums = albumService.findAll();
        renderizarFormulario(
                ctx,
                MusicaFormView.cadastro(new MusicaForm("", "", null, null, null, null, null), albums),
                null
        );
    }

    private void cadastrar(Context ctx) {
        MusicaForm form = extrairFormulario(ctx);
        try {
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            if (usuarioId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED).redirect("/login");
                return;
            }
            Integer notaVal = null;
            if (form.nota() != null && !form.nota().isBlank()) {
                notaVal = Integer.parseInt(form.nota());
            }
            Musica musica = Musica.novo(
                    form.titulo(),
                    form.artista(),
                    null, // genero
                    null, // duracaoSegundos
                    form.resenha(),
                    notaVal,
                    form.spotifyUrl(),
                    null, // albumId
                    usuarioId
            );
            service.save(musica);
            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Formulário de cadastro de música inválido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            List<Album> albums = albumService.findAll();
            renderizarFormulario(ctx, MusicaFormView.cadastro(form, albums), e.getMessage());
        }
    }

    private void exibirEdicao(Context ctx) {
        String id = ctx.pathParam("id");
        Musica musica = service.findById(id)
                .orElseThrow(() -> new io.javalin.http.NotFoundResponse("Música não encontrada"));
        
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        if (usuarioId == null || !usuarioId.equals(musica.getUsuarioId())) {
            ctx.status(HttpStatus.FORBIDDEN).result("Você não tem permissão para editar esta música.");
            return;
        }

        List<Album> albums = albumService.findAll();
        renderizarFormulario(ctx, MusicaFormView.edicao(musica, albums), null);
    }

    private void atualizar(Context ctx) {
        String id = ctx.pathParam("id");
        MusicaForm form = extrairFormulario(ctx);
        try {
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            if (usuarioId == null) {
                ctx.status(HttpStatus.UNAUTHORIZED).redirect("/login");
                return;
            }
            Musica musicaExistente = service.findById(id)
                    .orElseThrow(() -> new io.javalin.http.NotFoundResponse("Música não encontrada"));
            
            if (!usuarioId.equals(musicaExistente.getUsuarioId())) {
                ctx.status(HttpStatus.FORBIDDEN).result("Você não tem permissão para atualizar esta música.");
                return;
            }

            Integer notaVal = null;
            if (form.nota() != null && !form.nota().isBlank()) {
                notaVal = Integer.parseInt(form.nota());
            }

            Musica musicaDetails = new Musica(
                    id,
                    form.titulo(),
                    form.artista(),
                    musicaExistente.getGenero(),
                    musicaExistente.getDuracaoSegundos(),
                    form.resenha(),
                    notaVal,
                    form.spotifyUrl(),
                    musicaExistente.getAlbumId(),
                    usuarioId
            );
            service.update(id, musicaDetails);
            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Formulário de edição de música inválido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            List<Album> albums = albumService.findAll();
            renderizarFormulario(ctx, MusicaFormView.edicao(id, form, albums), e.getMessage());
        }
    }

    private void excluir(Context ctx) {
        String id = ctx.pathParam("id");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        if (usuarioId == null) {
            ctx.status(HttpStatus.UNAUTHORIZED).redirect("/login");
            return;
        }
        Musica musicaExistente = service.findById(id)
                .orElseThrow(() -> new io.javalin.http.NotFoundResponse("Música não encontrada"));
        
        if (!usuarioId.equals(musicaExistente.getUsuarioId())) {
            ctx.status(HttpStatus.FORBIDDEN).result("Você não tem permissão para excluir esta música.");
            return;
        }

        service.deleteById(id);
        redirecionarParaListagem(ctx);
    }

    private MusicaForm extrairFormulario(Context ctx) {
        return new MusicaForm(
                ctx.formParam("titulo"),
                ctx.formParam("artista"),
                null,
                null,
                ctx.formParam("nota"),
                ctx.formParam("resenha"),
                ctx.formParam("spotifyUrl")
        ).normalizado();
    }

    private void renderizarFormulario(Context ctx, MusicaFormView view, String erro) {
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("form", view);
        if (Objects.nonNull(erro)) {
            modelo.put("erro", erro);
        }
        ctx.render("musicas/formulario", modelo);
    }

    private void redirecionarParaListagem(Context ctx) {
        ctx.status(HttpStatus.SEE_OTHER).header("Location", "/");
    }
}
