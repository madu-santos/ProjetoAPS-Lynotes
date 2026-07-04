package br.ufpb.dcx.projetos.musica.controllers;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.infra.database.DatabaseException;
import br.ufpb.dcx.projetos.musica.dto.MusicaDTO;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;
import br.ufpb.dcx.projetos.musica.views.MusicaDTOView;
import br.ufpb.dcx.projetos.playlist.services.PlaylistService;
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
    private final PlaylistService playlistService;

    public MusicaController(MusicaService service, AlbumService albumService, PlaylistService playlistService) {
        this.service = service;
        this.albumService = albumService;
        this.playlistService = playlistService;
    }

    public void exibirHomeOuLanding(Context ctx) {
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

    public void listar(Context ctx) {
        ctx.redirect("/");
    }

    public void exibirCadastro(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        List<Album> albums = albumService.findAll(usuarioId);
        List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService.findByUsuarioId(usuarioId);
        renderizarFormulario(
                ctx,
                MusicaDTOView.cadastro(new MusicaDTO("", "", null, null, null, null, null, null, null, null), albums,
                        playlists),
                null);
    }

    public void cadastrar(Context ctx) {
        MusicaDTO dto = extrairFormulario(ctx);
        try {
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            if (usuarioId == null) {
                ctx.redirect("/login");
                return;
            }

            Integer notaVal = null;
            if (dto.nota() != null && !dto.nota().isBlank()) {
                notaVal = Integer.parseInt(dto.nota());
            }

            if (dto.youtubeUrl() != null && !dto.youtubeUrl().isBlank()) {
                playlistService.extractVideoId(dto.youtubeUrl());
            }

            Musica musica = Musica.novo(
                    dto.titulo(),
                    dto.artista(),
                    null, // genero
                    null, // duracaoSegundos
                    dto.resenha(),
                    notaVal,
                    dto.spotifyUrl(),
                    dto.youtubeUrl(),
                    null, // albumId
                    usuarioId);
            service.save(musica);

            if (dto.youtubeUrl() != null && !dto.youtubeUrl().isBlank()) {
                String videoId = playlistService.extractVideoId(dto.youtubeUrl());
                if (dto.novaPlaylistNome() != null && !dto.novaPlaylistNome().isBlank()) {
                    String playlistId = playlistService.createPlaylist(dto.novaPlaylistNome(), usuarioId).getId();
                    playlistService.addItem(playlistId, dto.youtubeUrl(), videoId, usuarioId);
                } else if (dto.playlistId() != null && !dto.playlistId().isBlank()) {
                    playlistService.addItem(dto.playlistId(), dto.youtubeUrl(), videoId, usuarioId);
                }
                playlistService.addItem(playlistService.findOrCreateHiddenPlaylist(usuarioId).getId(),
                        dto.youtubeUrl(), videoId, usuarioId);
            }

            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("DTO de cadastro de música inválido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            List<Album> albums = albumService.findAll(ctx.sessionAttribute("usuarioLogadoId"));
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService
                    .findByUsuarioId(ctx.sessionAttribute("usuarioLogadoId"));
            renderizarFormulario(ctx, MusicaDTOView.cadastro(dto, albums, playlists), e.getMessage());
        } catch (DatabaseException e) {
            LOGGER.error("Erro de banco de dados ao cadastrar música. path={}", ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            List<Album> albums = albumService.findAll(ctx.sessionAttribute("usuarioLogadoId"));
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService
                    .findByUsuarioId(ctx.sessionAttribute("usuarioLogadoId"));
            renderizarFormulario(ctx, MusicaDTOView.cadastro(dto, albums, playlists),
                    "Erro ao salvar a música. Tente novamente.");
        } catch (Exception e) {
            LOGGER.error("Erro inesperado ao cadastrar música. path={}", ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            List<Album> albums = albumService.findAll(ctx.sessionAttribute("usuarioLogadoId"));
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService
                    .findByUsuarioId(ctx.sessionAttribute("usuarioLogadoId"));
            renderizarFormulario(ctx, MusicaDTOView.cadastro(dto, albums, playlists),
                    "Ocorreu um erro inesperado. Tente novamente.");
        }
    }

    public void exibirEdicao(Context ctx) {
        String id = ctx.pathParam("id");
        Musica musica = service.findById(id)
                .orElseThrow(() -> new io.javalin.http.NotFoundResponse("Música não encontrada"));

        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        if (usuarioId == null || !usuarioId.equals(musica.getUsuarioId())) {
            ctx.status(HttpStatus.FORBIDDEN).result("Você não tem permissão para editar esta música.");
            return;
        }

        List<Album> albums = albumService.findAll(usuarioId);
        List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService.findByUsuarioId(usuarioId);
        renderizarFormulario(ctx, MusicaDTOView.edicao(musica, albums, playlists), null);
    }

    public void atualizar(Context ctx) {
        String id = ctx.pathParam("id");
        MusicaDTO dto = extrairFormulario(ctx);
        try {
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            if (usuarioId == null) {
                ctx.redirect("/login");
                return;
            }
            Musica musicaExistente = service.findById(id)
                    .orElseThrow(() -> new io.javalin.http.NotFoundResponse("Música não encontrada"));

            if (!usuarioId.equals(musicaExistente.getUsuarioId())) {
                ctx.status(HttpStatus.FORBIDDEN).result("Você não tem permissão para atualizar esta música.");
                return;
            }

            Integer notaVal = null;
            if (dto.nota() != null && !dto.nota().isBlank()) {
                notaVal = Integer.parseInt(dto.nota());
            }

            Musica musicaDetails = new Musica(
                    id,
                    dto.titulo(),
                    dto.artista(),
                    musicaExistente.getGenero(),
                    musicaExistente.getDuracaoSegundos(),
                    dto.resenha(),
                    notaVal,
                    dto.spotifyUrl(),
                    dto.youtubeUrl(),
                    musicaExistente.getAlbumId(),
                    usuarioId);
            service.update(id, musicaDetails);

            if (dto.youtubeUrl() != null && !dto.youtubeUrl().isBlank()) {
                String videoId = playlistService.extractVideoId(dto.youtubeUrl());
                if (dto.novaPlaylistNome() != null && !dto.novaPlaylistNome().isBlank()) {
                    String playlistId = playlistService.createPlaylist(dto.novaPlaylistNome(), usuarioId).getId();
                    playlistService.addItem(playlistId, dto.youtubeUrl(), videoId, usuarioId);
                } else if (dto.playlistId() != null && !dto.playlistId().isBlank()) {
                    playlistService.addItem(dto.playlistId(), dto.youtubeUrl(), videoId, usuarioId);
                }
                playlistService.addItem(playlistService.findOrCreateHiddenPlaylist(usuarioId).getId(),
                        dto.youtubeUrl(), videoId, usuarioId);
            }

            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("DTO de edição de música inválido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            List<Album> albums = albumService.findAll(ctx.sessionAttribute("usuarioLogadoId"));
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService
                    .findByUsuarioId(ctx.sessionAttribute("usuarioLogadoId"));
            renderizarFormulario(ctx, MusicaDTOView.edicao(id, dto, albums, playlists), e.getMessage());
        } catch (DatabaseException e) {
            LOGGER.error("Erro de banco de dados ao atualizar música. path={}", ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            List<Album> albums = albumService.findAll(ctx.sessionAttribute("usuarioLogadoId"));
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService
                    .findByUsuarioId(ctx.sessionAttribute("usuarioLogadoId"));
            renderizarFormulario(ctx, MusicaDTOView.edicao(id, dto, albums, playlists),
                    "Erro ao salvar as alterações. Tente novamente.");
        } catch (Exception e) {
            LOGGER.error("Erro inesperado ao atualizar música. path={}", ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            List<Album> albums = albumService.findAll(ctx.sessionAttribute("usuarioLogadoId"));
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService
                    .findByUsuarioId(ctx.sessionAttribute("usuarioLogadoId"));
            renderizarFormulario(ctx, MusicaDTOView.edicao(id, dto, albums, playlists),
                    "Ocorreu um erro inesperado. Tente novamente.");
        }
    }

    public void excluir(Context ctx) {
        String id = ctx.pathParam("id");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        if (usuarioId == null) {
            ctx.redirect("/login");
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

    private MusicaDTO extrairFormulario(Context ctx) {
        return new MusicaDTO(
                ctx.formParam("titulo"),
                ctx.formParam("artista"),
                null,
                null,
                ctx.formParam("nota"),
                ctx.formParam("resenha"),
                ctx.formParam("spotifyUrl"),
                ctx.formParam("youtubeUrl"),
                ctx.formParam("playlistId"),
                ctx.formParam("novaPlaylistNome")).normalizado();
    }

    private void renderizarFormulario(Context ctx, MusicaDTOView view, String erro) {
        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("form", view);
        if (Objects.nonNull(erro)) {
            modelo.put("erro", erro);
        }
        ctx.render("musicas/formulario", modelo);
    }

    private void redirecionarParaListagem(Context ctx) {
        ctx.redirect("/", HttpStatus.SEE_OTHER);
    }
}
