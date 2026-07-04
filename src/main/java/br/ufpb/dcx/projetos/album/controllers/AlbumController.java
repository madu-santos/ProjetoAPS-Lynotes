package br.ufpb.dcx.projetos.album.controllers;

import br.ufpb.dcx.projetos.album.dto.AlbumDTO;
import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.album.views.AlbumDTOView;
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

    public void listar(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        List<Album> albums = service.findAll(usuarioId);
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("albums", albums);
        modelo.put("semAlbums", albums.isEmpty());
        ctx.render("albums/lista", modelo);
    }

    public void exibirCadastro(Context ctx) {
        renderizarFormulario(
                ctx,
                AlbumDTOView.cadastro(new AlbumDTO("", "", "")),
                null
        );
    }

    public void cadastrar(Context ctx) {
        AlbumDTO dto = extrairFormulario(ctx);
        try {
            int ano = parseAno(dto.anoLancamento());
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            Album album = Album.novo(dto.titulo(), dto.artista(), ano, usuarioId);
            service.save(album, usuarioId);
            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("DTO de cadastro de álbum inválido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            renderizarFormulario(ctx, AlbumDTOView.cadastro(dto), e.getMessage());
        }
    }

    public void exibirEdicao(Context ctx) {
        String id = ctx.pathParam("id");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        Album album = service.findById(id, usuarioId)
                .orElseThrow(() -> new io.javalin.http.NotFoundResponse("Álbum não encontrado"));
        renderizarFormulario(ctx, AlbumDTOView.edicao(album), null);
    }

    public void atualizar(Context ctx) {
        String id = ctx.pathParam("id");
        AlbumDTO dto = extrairFormulario(ctx);
        try {
            int ano = parseAno(dto.anoLancamento());
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            Album albumDetails = new Album(id, dto.titulo(), dto.artista(), ano, usuarioId);
            service.update(id, albumDetails, usuarioId);
            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("DTO de edição de álbum inválido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            renderizarFormulario(ctx, AlbumDTOView.edicao(id, dto), e.getMessage());
        }
    }

    public void excluir(Context ctx) {
        String id = ctx.pathParam("id");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        service.deleteById(id, usuarioId);
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

    private AlbumDTO extrairFormulario(Context ctx) {
        return new AlbumDTO(
                ctx.formParam("titulo"),
                ctx.formParam("artista"),
                ctx.formParam("anoLancamento")
        );
    }

    private void renderizarFormulario(Context ctx, AlbumDTOView view, String erro) {
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
