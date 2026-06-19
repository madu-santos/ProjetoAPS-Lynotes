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
    public static final String ROTA_NOVO = "/musicas/new"; // Matches the original route defined in list.html
    public static final String ROTA_EDICAO = "/musicas/edit/{id}"; // Matches list.html format
    public static final String ROTA_ATUALIZACAO = "/musicas/edit/{id}";
    public static final String ROTA_EXCLUSAO = "/musicas/delete/{id}"; // Matches list.html format

    private static final Logger LOGGER = LoggerFactory.getLogger(MusicaController.class);

    private final MusicaService service;
    private final AlbumService albumService;

    public MusicaController(MusicaService service, AlbumService albumService) {
        this.service = service;
        this.albumService = albumService;
    }

    public void registrarRotas(Javalin app) {
        app.get(ROTA_LISTAGEM, this::listar);
        app.get(ROTA_NOVO, this::exibirCadastro);
        app.post(ROTA_LISTAGEM, this::cadastrar);
        app.get(ROTA_EDICAO, this::exibirEdicao);
        app.post(ROTA_ATUALIZACAO, this::atualizar);
        app.post(ROTA_EXCLUSAO, this::excluir);
    }

    private void listar(Context ctx) {
        List<Musica> musicas = service.findAll();
        List<MusicaExibicao> musicasExibicao = musicas.stream()
                .map(m -> {
                    String albumTitulo = "N/A";
                    if (m.getAlbumId() != null && !m.getAlbumId().isBlank()) {
                        albumTitulo = albumService.findById(m.getAlbumId())
                                .map(Album::getTitulo)
                                .orElse("N/A");
                    }
                    return new MusicaExibicao(m.getId(), m.getTitulo(), m.getGenero(), m.getDuracaoSegundos(), albumTitulo);
                })
                .toList();

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("musicas", musicasExibicao);
        modelo.put("semMusicas", musicasExibicao.isEmpty());
        ctx.render("musicas/lista", modelo);
    }

    private void exibirCadastro(Context ctx) {
        List<Album> albums = albumService.findAll();
        renderizarFormulario(
                ctx,
                MusicaFormView.cadastro(new MusicaForm("", "", "", ""), albums),
                null
        );
    }

    private void cadastrar(Context ctx) {
        MusicaForm form = extrairFormulario(ctx);
        try {
            int duracao = parseDuracao(form.duracaoSegundos());
            Musica musica = Musica.novo(form.titulo(), form.genero(), duracao, form.albumId());
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
        List<Album> albums = albumService.findAll();
        renderizarFormulario(ctx, MusicaFormView.edicao(musica, albums), null);
    }

    private void atualizar(Context ctx) {
        String id = ctx.pathParam("id");
        MusicaForm form = extrairFormulario(ctx);
        try {
            int duracao = parseDuracao(form.duracaoSegundos());
            Musica musicaDetails = new Musica(id, form.titulo(), form.genero(), duracao, form.albumId());
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
        service.deleteById(id);
        redirecionarParaListagem(ctx);
    }

    private int parseDuracao(String duracaoStr) {
        if (duracaoStr == null || duracaoStr.trim().isEmpty()) {
            throw new IllegalArgumentException("A duração da música é obrigatória");
        }
        try {
            return Integer.parseInt(duracaoStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("A duração da música deve ser um número inteiro válido");
        }
    }

    private MusicaForm extrairFormulario(Context ctx) {
        return new MusicaForm(
                ctx.formParam("titulo"),
                ctx.formParam("genero"),
                ctx.formParam("duracaoSegundos"),
                ctx.formParam("albumId")
        );
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
        ctx.status(HttpStatus.SEE_OTHER).header("Location", ROTA_LISTAGEM);
    }

    public static record MusicaExibicao(
            String id,
            String titulo,
            String genero,
            int duracaoSegundos,
            String albumTitulo
    ) {}
}
