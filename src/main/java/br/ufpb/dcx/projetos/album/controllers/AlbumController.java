package br.ufpb.dcx.projetos.album.controllers;

import br.ufpb.dcx.projetos.album.dto.AlbumForm;
import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.album.views.AlbumFormView;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AlbumController {

    public static final String ROTA_LISTAGEM = "/albums";
    public static final String ROTA_NOVO = "/albums/novo";
    public static final String ROTA_EDICAO = "/albums/{id}/editar";
    public static final String ROTA_ATUALIZACAO = "/albums/{id}";
    public static final String ROTA_EXCLUSAO = "/albums/{id}/excluir";

    private static final Logger LOGGER = LoggerFactory.getLogger(AlbumController.class);

    private final AlbumService service;

    public AlbumController(AlbumService service) {
        this.service = service;
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
        List<Album> albums = service.findAll();
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("albums", albums);
        modelo.put("semAlbums", albums.isEmpty());
        ctx.render("albums/lista", modelo);
    }

    private void exibirCadastro(Context ctx) {
        renderizarFormulario(
                ctx,
                AlbumFormView.cadastro(new AlbumForm("", "", "")),
                null
        );
    }

    private void cadastrar(Context ctx) {
        AlbumForm form = extrairFormulario(ctx);
        try {
            int ano = parseAno(form.anoLancamento());
            Album album = Album.novo(form.titulo(), form.artista(), ano);
            service.save(album);
            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Formulário de cadastro de álbum inválido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            renderizarFormulario(ctx, AlbumFormView.cadastro(form), e.getMessage());
        }
    }

    private void exibirEdicao(Context ctx) {
        String id = ctx.pathParam("id");
        Album album = service.findById(id)
                .orElseThrow(() -> new io.javalin.http.NotFoundResponse("Álbum não encontrado"));
        renderizarFormulario(ctx, AlbumFormView.edicao(album), null);
    }

    private void atualizar(Context ctx) {
        String id = ctx.pathParam("id");
        AlbumForm form = extrairFormulario(ctx);
        try {
            int ano = parseAno(form.anoLancamento());
            Album albumDetails = new Album(id, form.titulo(), form.artista(), ano);
            service.update(id, albumDetails);
            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Formulário de edição de álbum inválido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            renderizarFormulario(ctx, AlbumFormView.edicao(id, form), e.getMessage());
        }
    }

    private void excluir(Context ctx) {
        String id = ctx.pathParam("id");
        service.deleteById(id);
        redirecionarParaListagem(ctx);
    }

    private int parseAno(String anoStr) {
        if (anoStr == null || anoStr.trim().isEmpty()) {
            throw new IllegalArgumentException("O ano de lançamento é obrigatório");
        }
        try {
            return Integer.parseInt(anoStr.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("O ano de lançamento deve ser um número inteiro válido");
        }
    }

    private AlbumForm extrairFormulario(Context ctx) {
        return new AlbumForm(
                ctx.formParam("titulo"),
                ctx.formParam("artista"),
                ctx.formParam("anoLancamento")
        );
    }

    private void renderizarFormulario(Context ctx, AlbumFormView view, String erro) {
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("form", view);
        if (Objects.nonNull(erro)) {
            modelo.put("erro", erro);
        }
        ctx.render("albums/formulario", modelo);
    }

    private void redirecionarParaListagem(Context ctx) {
        ctx.redirect(ROTA_LISTAGEM, HttpStatus.SEE_OTHER);
    }
}
