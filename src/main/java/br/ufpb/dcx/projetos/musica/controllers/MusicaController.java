package br.ufpb.dcx.projetos.musica.controllers;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.infra.database.DatabaseException;
import br.ufpb.dcx.projetos.musica.dto.MusicaDTO;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;
import br.ufpb.dcx.projetos.musica.views.MusicaDTOView;
import br.ufpb.dcx.projetos.playlist.services.PlaylistService;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
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
    private final ArtistaService artistaService;
    private final br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService;

    public MusicaController(MusicaService service, AlbumService albumService, PlaylistService playlistService, ArtistaService artistaService, br.ufpb.dcx.projetos.comunidade.services.ComunidadeService comunidadeService) {
        this.service = service;
        this.albumService = albumService;
        this.playlistService = playlistService;
        this.artistaService = artistaService;
        this.comunidadeService = comunidadeService;
    }

    @io.javalin.openapi.OpenApi(
            summary = "PÃ¡gina inicial ou landing page",
            operationId = "home",
            path = "/",
            methods = io.javalin.openapi.HttpMethod.GET,
            tags = {"MÃºsicas"},
            responses = {
                    @io.javalin.openapi.OpenApiResponse(status = "200", description = "PÃ¡gina renderizada com sucesso")
            }
    )
    public void exibirHomeOuLanding(Context ctx) {
        String usuarioLogado = ctx.sessionAttribute("usuarioLogado");
        if (usuarioLogado != null) {
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            List<Musica> musicas = service.findByUsuarioId(usuarioId);
            List<Artista> artistas = artistaService.listar(null, usuarioId);
            List<Album> albums = albumService.findAll(usuarioId);
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService.findByUsuarioId(usuarioId);

            Map<String, String> playlistCovers = new HashMap<>();
            for (br.ufpb.dcx.projetos.playlist.models.Playlist pl : playlists) {
                playlistCovers.put(pl.getId(), playlistService.getPlaylistCapaUrl(pl.getId()));
            }

            Map<String, Object> modelo = new HashMap<>();
            modelo.put("usuarioLogado", usuarioLogado);
            modelo.put("musicas", musicas);
            modelo.put("semMusicas", musicas.isEmpty());
            modelo.put("artistas", artistas);
            modelo.put("semArtistas", artistas.isEmpty());
            modelo.put("albums", albums);
            modelo.put("semAlbums", albums.isEmpty());
            modelo.put("playlists", playlists);
            modelo.put("playlistCovers", playlistCovers);
            ctx.render("home", modelo);
        } else {
            ctx.render("index", Map.of());
        }
    }

    public void listar(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String busca = ctx.queryParam("busca");
        List<Musica> musicas = service.search(busca, usuarioId);
        List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService.findByUsuarioId(usuarioId);

        Map<String, String> playlistCovers = new HashMap<>();
        for (br.ufpb.dcx.projetos.playlist.models.Playlist pl : playlists) {
            playlistCovers.put(pl.getId(), playlistService.getPlaylistCapaUrl(pl.getId()));
        }

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("musicas", musicas);
        modelo.put("semMusicas", musicas.isEmpty() && (busca == null || busca.trim().isEmpty()));
        modelo.put("playlists", playlists);
        modelo.put("playlistCovers", playlistCovers);
        modelo.put("busca", busca != null ? busca : "");
        modelo.put("buscaAtiva", busca != null && !busca.trim().isEmpty());
        ctx.render("musicas/lista", modelo);
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

    @io.javalin.openapi.OpenApi(
            summary = "Catalogar nova mÃºsica",
            operationId = "cadastrarMusica",
            path = "/musicas",
            methods = io.javalin.openapi.HttpMethod.POST,
            tags = {"MÃºsicas"},
            responses = {
                    @io.javalin.openapi.OpenApiResponse(status = "303", description = "Redireciona para a home"),
                    @io.javalin.openapi.OpenApiResponse(status = "422", description = "Dados invÃ¡lidos")
            }
    )
    public void cadastrar(Context ctx) {
        MusicaDTO dto = extrairFormulario(ctx);
        try {
            dto.validar();
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            if (usuarioId == null) {
                ctx.redirect("/login");
                return;
            }

            Double notaVal = null;
            if (dto.nota() != null && !dto.nota().isBlank()) {
                notaVal = Double.parseDouble(dto.nota());
            }

            Integer duracaoVal = null;
            if (dto.youtubeUrl() != null && !dto.youtubeUrl().isBlank()) {
                playlistService.extractVideoId(dto.youtubeUrl()); // throws if invalid
                duracaoVal = playlistService.fetchYouTubeDuration(dto.youtubeUrl());
            }

            Musica musica = Musica.novo(
                    dto.titulo(),
                    dto.artista(),
                    null, // genero
                    duracaoVal, // duracaoSegundos
                    dto.resenha(),
                    notaVal,
                    dto.spotifyUrl(),
                    dto.youtubeUrl(),
                    null, // albumId
                    usuarioId);
            service.save(musica);

            String compartilhar = ctx.formParam("compartilharComunidade");
            String mensagem = ctx.formParam("mensagemComunidade");

            if (comunidadeService != null && "on".equals(compartilhar)) {
                String msg = "Catalogou a mÃºsica: **" + musica.getTitulo() + "** (" + musica.getArtista() + ")";
                if (musica.getNota() != null) {
                    msg += " e avaliou com " + musica.getNota() + " estrelas. â­";
                }
                if (mensagem != null && !mensagem.trim().isEmpty()) {
                    msg += "\n\n\"" + mensagem.trim() + "\"";
                }
                comunidadeService.criarPostCompartilhado(usuarioId, "SHARE_MUSIC", musica.getId(), msg);
            }

            if (dto.youtubeUrl() != null && !dto.youtubeUrl().isBlank()) {
                String videoId = playlistService.extractVideoId(dto.youtubeUrl());
                try {
                    if (dto.novaPlaylistNome() != null && !dto.novaPlaylistNome().isBlank()) {
                        String playlistId = playlistService.createPlaylist(dto.novaPlaylistNome(), usuarioId).getId();
                        playlistService.addItem(playlistId, dto.youtubeUrl(), videoId, usuarioId);
                    } else if (dto.playlistId() != null && !dto.playlistId().isBlank()) {
                        playlistService.addItem(dto.playlistId(), dto.youtubeUrl(), videoId, usuarioId);
                    }
                } catch (IllegalArgumentException ignored) {}
                try {
                    playlistService.addItem(playlistService.findOrCreateHiddenPlaylist(usuarioId).getId(),
                            dto.youtubeUrl(), videoId, usuarioId);
                } catch (IllegalArgumentException ignored) {}
            }

            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("DTO de cadastro de mÃºsica invÃ¡lido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            List<Album> albums = albumService.findAll(ctx.sessionAttribute("usuarioLogadoId"));
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService
                    .findByUsuarioId(ctx.sessionAttribute("usuarioLogadoId"));
            renderizarFormulario(ctx, MusicaDTOView.cadastro(dto, albums, playlists), e.getMessage());
        } catch (DatabaseException e) {
            LOGGER.error("Erro de banco de dados ao cadastrar mÃºsica. path={}", ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            List<Album> albums = albumService.findAll(ctx.sessionAttribute("usuarioLogadoId"));
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService
                    .findByUsuarioId(ctx.sessionAttribute("usuarioLogadoId"));
            renderizarFormulario(ctx, MusicaDTOView.cadastro(dto, albums, playlists),
                    "Erro ao salvar a mÃºsica. Tente novamente.");
        } catch (Exception e) {
            LOGGER.error("Erro inesperado ao cadastrar mÃºsica. path={}", ctx.path(), e);
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
                .orElseThrow(() -> new io.javalin.http.NotFoundResponse("MÃºsica nÃ£o encontrada"));

        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        if (usuarioId == null || !usuarioId.equals(musica.getUsuarioId())) {
            ctx.status(HttpStatus.FORBIDDEN).result("VocÃª nÃ£o tem permissÃ£o para editar esta mÃºsica.");
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
            dto.validar();
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            if (usuarioId == null) {
                ctx.redirect("/login");
                return;
            }
            Musica musicaExistente = service.findById(id)
                    .orElseThrow(() -> new io.javalin.http.NotFoundResponse("MÃºsica nÃ£o encontrada"));

            if (!usuarioId.equals(musicaExistente.getUsuarioId())) {
                ctx.status(HttpStatus.FORBIDDEN).result("VocÃª nÃ£o tem permissÃ£o para atualizar esta mÃºsica.");
                return;
            }

            Double notaVal = null;
            if (dto.nota() != null && !dto.nota().isBlank()) {
                notaVal = Double.parseDouble(dto.nota());
            }

            Integer duracaoVal = musicaExistente.getDuracaoSegundos();
            if (dto.youtubeUrl() != null && !dto.youtubeUrl().isBlank()) {
                // If they provided a new URL or kept the existing one, we can fetch its duration.
                duracaoVal = playlistService.fetchYouTubeDuration(dto.youtubeUrl());
            }

            Musica musicaDetails = new Musica(
                    id,
                    dto.titulo(),
                    dto.artista(),
                    musicaExistente.getGenero(),
                    duracaoVal,
                    dto.resenha(),
                    notaVal,
                    dto.spotifyUrl(),
                    dto.youtubeUrl(),
                    musicaExistente.getAlbumId(),
                    usuarioId);
            service.update(id, musicaDetails);

            String compartilhar = ctx.formParam("compartilharComunidade");
            String mensagem = ctx.formParam("mensagemComunidade");

            if (comunidadeService != null && "on".equals(compartilhar)) {
                String msg = "Atualizou a mÃºsica: **" + musicaDetails.getTitulo() + "** (" + musicaDetails.getArtista() + ")";
                if (musicaDetails.getNota() != null) {
                    msg += " e avaliou com " + musicaDetails.getNota() + " estrelas. â­";
                }
                if (mensagem != null && !mensagem.trim().isEmpty()) {
                    msg += "\n\n\"" + mensagem.trim() + "\"";
                }
                comunidadeService.criarPostCompartilhado(usuarioId, "SHARE_MUSIC", id, msg);
            }

            if (dto.youtubeUrl() != null && !dto.youtubeUrl().isBlank()) {
                String videoId = playlistService.extractVideoId(dto.youtubeUrl());
                try {
                    if (dto.novaPlaylistNome() != null && !dto.novaPlaylistNome().isBlank()) {
                        String playlistId = playlistService.createPlaylist(dto.novaPlaylistNome(), usuarioId).getId();
                        playlistService.addItem(playlistId, dto.youtubeUrl(), videoId, usuarioId);
                    } else if (dto.playlistId() != null && !dto.playlistId().isBlank()) {
                        playlistService.addItem(dto.playlistId(), dto.youtubeUrl(), videoId, usuarioId);
                    }
                } catch (IllegalArgumentException ignored) {}
                try {
                    playlistService.addItem(playlistService.findOrCreateHiddenPlaylist(usuarioId).getId(),
                            dto.youtubeUrl(), videoId, usuarioId);
                } catch (IllegalArgumentException ignored) {}
            }

            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("DTO de ediÃ§Ã£o de mÃºsica invÃ¡lido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            List<Album> albums = albumService.findAll(ctx.sessionAttribute("usuarioLogadoId"));
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService
                    .findByUsuarioId(ctx.sessionAttribute("usuarioLogadoId"));
            renderizarFormulario(ctx, MusicaDTOView.edicao(id, dto, albums, playlists), e.getMessage());
        } catch (DatabaseException e) {
            LOGGER.error("Erro de banco de dados ao atualizar mÃºsica. path={}", ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            List<Album> albums = albumService.findAll(ctx.sessionAttribute("usuarioLogadoId"));
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService
                    .findByUsuarioId(ctx.sessionAttribute("usuarioLogadoId"));
            renderizarFormulario(ctx, MusicaDTOView.edicao(id, dto, albums, playlists),
                    "Erro ao salvar as alteraÃ§Ãµes. Tente novamente.");
        } catch (Exception e) {
            LOGGER.error("Erro inesperado ao atualizar mÃºsica. path={}", ctx.path(), e);
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
                .orElseThrow(() -> new io.javalin.http.NotFoundResponse("MÃºsica nÃ£o encontrada"));

        if (!usuarioId.equals(musicaExistente.getUsuarioId())) {
            ctx.status(HttpStatus.FORBIDDEN).result("VocÃª nÃ£o tem permissÃ£o para excluir esta mÃºsica.");
            return;
        }

        service.deleteById(id);
        redirecionarParaListagem(ctx);
    }

    private MusicaDTO extrairFormulario(Context ctx) {
        return new MusicaDTO(
                ctx.formParam("titulo"),
                ctx.formParam("artista"),
                ctx.formParam("duracaoSegundos"),
                ctx.formParam("albumId"),
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
