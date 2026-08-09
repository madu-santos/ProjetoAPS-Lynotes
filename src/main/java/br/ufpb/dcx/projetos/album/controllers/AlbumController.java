package br.ufpb.dcx.projetos.album.controllers;

import br.ufpb.dcx.projetos.album.dto.AlbumDTO;
import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.album.views.AlbumDTOView;
import br.ufpb.dcx.projetos.musica.services.MusicaService;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class AlbumController {

    public static final String ROTA_LISTAGEM = "/albuns";
    public static final String ROTA_NOVO = "/albuns/novo";
    public static final String ROTA_EDICAO = "/albuns/{id}/editar";
    public static final String ROTA_ATUALIZACAO = "/albuns/{id}";
    public static final String ROTA_EXCLUSAO = "/albuns/{id}/excluir";

    private static final Logger LOGGER = LoggerFactory.getLogger(AlbumController.class);

    private final AlbumService service;
    private final br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService;
    private final MusicaService musicaService;

    public AlbumController(AlbumService service, br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService) {
        this(service, comunidadeService, null);
    }

    public AlbumController(AlbumService service, br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService, MusicaService musicaService) {
        this.service = service;
        this.comunidadeService = comunidadeService;
        this.musicaService = musicaService;
    }

    @io.javalin.openapi.OpenApi(
            summary = "Listar Ã¡lbuns",
            operationId = "listarAlbums",
            path = "/albums",
            methods = io.javalin.openapi.HttpMethod.GET,
            tags = {"Ãlbuns"},
            responses = {
                    @io.javalin.openapi.OpenApiResponse(status = "200", description = "Lista de Ã¡lbuns retornada com sucesso")
            }
    )
    public void listar(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String busca = ctx.queryParam("busca");
        List<Album> albums = service.search(busca, usuarioId);
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("albums", albums);
        modelo.put("semAlbums", albums.isEmpty() && (busca == null || busca.trim().isEmpty()));
        modelo.put("busca", busca != null ? busca : "");
        modelo.put("buscaAtiva", busca != null && !busca.trim().isEmpty());
        ctx.render("albums/lista", modelo);
    }

    public void exibirCadastro(Context ctx) {
        renderizarFormulario(
                ctx,
                AlbumDTOView.cadastro(new AlbumDTO("", "", "")),
                null
        );
    }

    @io.javalin.openapi.OpenApi(
            summary = "Cadastrar novo Ã¡lbum",
            operationId = "cadastrarAlbum",
            path = "/albums",
            methods = io.javalin.openapi.HttpMethod.POST,
            tags = {"Ãlbuns"},
            responses = {
                    @io.javalin.openapi.OpenApiResponse(status = "303", description = "Redireciona para a listagem"),
                    @io.javalin.openapi.OpenApiResponse(status = "422", description = "Dados invÃ¡lidos")
            }
    )
    public void cadastrar(Context ctx) {
        AlbumDTO dto = extrairFormulario(ctx);
        try {
            dto.validar();
            int ano = Integer.parseInt(dto.anoLancamento().trim());
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            Album album = Album.novo(dto.titulo(), dto.artista(), ano, dto.nota(), usuarioId, dto.youtubeId(), dto.capaUrl(), dto.resenha());
            service.save(album, usuarioId);
            
            if (dto.youtubeId() != null && dto.youtubeId().startsWith("itunes_album_")) {
                String collectionId = dto.youtubeId().replace("itunes_album_", "");
                musicaService.importarFaixasAlbumItunes(collectionId, album.getId(), usuarioId, album.getCapaUrl());
            } else {
                String ytId = dto.youtubeId();
                if (ytId != null && !ytId.isBlank()) {
                    String playlistId = null;
                    if (ytId.startsWith("yt_playlist_")) {
                        playlistId = ytId.replace("yt_playlist_", "");
                    } else if (ytId.startsWith("PL") || ytId.startsWith("OL") || ytId.startsWith("RD")) {
                        playlistId = ytId;
                    } else if (ytId.contains("list=")) {
                        int idx = ytId.indexOf("list=");
                        int end = ytId.indexOf('&', idx);
                        playlistId = (end > 0) ? ytId.substring(idx + 5, end) : ytId.substring(idx + 5);
                    }
                    if (playlistId != null && !playlistId.isBlank()) {
                        musicaService.importarFaixasAlbumYouTube(playlistId, album.getId(), usuarioId, album.getCapaUrl());
                    }
                }
            }
            
            String compartilhar = ctx.formParam("compartilharNaComunidade");
            if (comunidadeService != null && ("on".equals(compartilhar) || "true".equals(compartilhar))) {
                String opiniao = dto.resenha();
                if (opiniao == null) {
                    opiniao = "";
                }
                comunidadeService.criarPostCompartilhado(usuarioId, "SHARE_ALBUM", album.getId(), opiniao);
            }
            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("DTO de cadastro de Ã¡lbum invÃ¡lido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            renderizarFormulario(ctx, AlbumDTOView.cadastro(dto), e.getMessage());
        }
    }

    public void exibirDetalhes(Context ctx) {
        String id = ctx.pathParam("id");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        Album album = service.findById(id, usuarioId)
                .orElseThrow(() -> new io.javalin.http.NotFoundResponse("Álbum não encontrado"));
        List<br.ufpb.dcx.projetos.musica.models.Musica> faixas = musicaService != null ? musicaService.findByAlbumId(id) : java.util.Collections.emptyList();

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("album", album);
        modelo.put("faixas", faixas);
        ctx.render("albums/detalhes", modelo);
    }

    public void exibirEdicao(Context ctx) {
        String id = ctx.pathParam("id");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        Album album = service.findById(id, usuarioId)
                .orElseThrow(() -> new io.javalin.http.NotFoundResponse("Ãlbum nÃ£o encontrado"));
        renderizarFormulario(ctx, AlbumDTOView.edicao(album), null);
    }

    @io.javalin.openapi.OpenApi(
            summary = "Atualizar Ã¡lbum",
            operationId = "atualizarAlbum",
            path = "/albums/{id}",
            methods = io.javalin.openapi.HttpMethod.POST,
            tags = {"Ãlbuns"},
            pathParams = {
                    @io.javalin.openapi.OpenApiParam(name = "id", description = "ID do Ã¡lbum")
            },
            responses = {
                    @io.javalin.openapi.OpenApiResponse(status = "303", description = "Redireciona para a listagem"),
                    @io.javalin.openapi.OpenApiResponse(status = "404", description = "Ãlbum nÃ£o encontrado"),
                    @io.javalin.openapi.OpenApiResponse(status = "422", description = "Dados invÃ¡lidos")
            }
    )
    public void atualizar(Context ctx) {
        String id = ctx.pathParam("id");
        AlbumDTO dto = extrairFormulario(ctx);
        try {
            dto.validar();
            int ano = Integer.parseInt(dto.anoLancamento().trim());
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            Album albumDetails = Album.novo(dto.titulo(), dto.artista(), ano, dto.nota(), usuarioId, dto.youtubeId(), dto.capaUrl(), dto.resenha());
            service.update(id, albumDetails, usuarioId);
            
            String compartilhar = ctx.formParam("compartilharNaComunidade");
            if (comunidadeService != null && ("on".equals(compartilhar) || "true".equals(compartilhar))) {
                String opiniao = dto.resenha();
                if (opiniao == null) {
                    opiniao = "";
                }
                comunidadeService.criarPostCompartilhado(usuarioId, "SHARE_ALBUM", id, opiniao);
            }
            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("DTO de ediÃ§Ã£o de Ã¡lbum invÃ¡lido. path={} mensagem={}", ctx.path(), e.getMessage());
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

    public void listarFaixasApi(Context ctx) {
        String id = ctx.pathParam("id");
        ctx.json(musicaService.findByAlbumId(id));
    }

    public void alternarOcultaFaixa(Context ctx) {
        String albumId = ctx.pathParam("albumId");
        String faixaId = ctx.pathParam("faixaId");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        if (musicaService != null) {
            musicaService.alternarOculta(faixaId, usuarioId);
        }
        ctx.redirect("/albuns/" + albumId, HttpStatus.SEE_OTHER);
    }

    private AlbumDTO extrairFormulario(Context ctx) {
        String notaStr = ctx.formParam("nota");
        Double nota = (notaStr != null && !notaStr.trim().isEmpty()) ? Double.parseDouble(notaStr.trim()) : null;
        return new AlbumDTO(
                ctx.formParam("titulo"),
                ctx.formParam("artista"),
                ctx.formParam("anoLancamento"),
                nota,
                ctx.formParam("youtubeId"),
                ctx.formParam("capaUrl"),
                ctx.formParam("resenha")
        ).normalizado();
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
        String targetUrl = ctx.header("HX-Current-URL");
        if (targetUrl == null || targetUrl.isBlank()) {
            targetUrl = ctx.header("Referer");
        }
        if (targetUrl != null && !targetUrl.isBlank()) {
            try {
                java.net.URI uri = java.net.URI.create(targetUrl);
                String path = uri.getPath();
                if (path != null && (path.equals(ROTA_LISTAGEM) || path.equals("/"))) {
                    String query = uri.getQuery();
                    String finalTarget = path + (query != null && !query.isBlank() ? "?" + query : "");
                    ctx.redirect(finalTarget, HttpStatus.SEE_OTHER);
                    return;
                }
            } catch (Exception ignored) {}
        }
        ctx.redirect(ROTA_LISTAGEM, HttpStatus.SEE_OTHER);
    }
}
