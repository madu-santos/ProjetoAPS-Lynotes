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

            Map<String, String> musicCovers = new HashMap<>();
            for (Musica m : musicas) {
                if (m.getYoutubeId() != null && m.getCapaUrl() != null && !m.getCapaUrl().isBlank()) {
                    musicCovers.put(m.getYoutubeId(), m.getCapaUrl());
                }
            }

            Map<String, List<String>> playlistCovers = new HashMap<>();
            for (br.ufpb.dcx.projetos.playlist.models.Playlist pl : playlists) {
                List<String> covers = java.util.Collections.emptyList();
                try {
                    var plWithItems = playlistService.findPlaylistWithItems(pl.getId(), usuarioId);
                    if (plWithItems != null && plWithItems.getItems() != null) {
                        covers = plWithItems.getItems().stream()
                                .limit(4)
                                .map(item -> {
                                    if (musicCovers.containsKey(item.getVideoId())) {
                                        return musicCovers.get(item.getVideoId());
                                    }
                                    return "https://img.youtube.com/vi/" + item.getVideoId() + "/hqdefault.jpg";
                                })
                                .toList();
                    }
                } catch (Exception ignored) {}
                playlistCovers.put(pl.getId(), covers);
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

        Map<String, String> musicCovers = new HashMap<>();
        for (Musica m : musicas) {
            if (m.getYoutubeId() != null && m.getCapaUrl() != null && !m.getCapaUrl().isBlank()) {
                musicCovers.put(m.getYoutubeId(), m.getCapaUrl());
            }
        }

        Map<String, List<String>> playlistCovers = new HashMap<>();
        for (br.ufpb.dcx.projetos.playlist.models.Playlist pl : playlists) {
            List<String> covers = java.util.Collections.emptyList();
            try {
                var plWithItems = playlistService.findPlaylistWithItems(pl.getId(), usuarioId);
                if (plWithItems != null && plWithItems.getItems() != null) {
                    covers = plWithItems.getItems().stream()
                            .limit(4)
                            .map(item -> {
                                if (musicCovers.containsKey(item.getVideoId())) {
                                    return musicCovers.get(item.getVideoId());
                                }
                                return "https://img.youtube.com/vi/" + item.getVideoId() + "/hqdefault.jpg";
                            })
                            .toList();
                }
            } catch (Exception ignored) {}
            playlistCovers.put(pl.getId(), covers);
        }

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("musicas", musicas.stream().limit(8).toList());
        modelo.put("totalMusicas", musicas.size());
        modelo.put("temMaisMusicas", musicas.size() > 8);
        modelo.put("semMusicas", musicas.isEmpty() && (busca == null || busca.trim().isEmpty()));
        modelo.put("playlists", playlists);
        modelo.put("playlistCovers", playlistCovers);
        modelo.put("busca", busca != null ? busca : "");
        modelo.put("buscaAtiva", busca != null && !busca.trim().isEmpty());
        ctx.render("musicas/lista", modelo);
    }

    public void listarTodas(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        String busca = ctx.queryParam("busca");
        List<Musica> musicas = service.search(busca, usuarioId);
        List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = playlistService.findByUsuarioId(usuarioId);

        Map<String, String> musicCovers = new HashMap<>();
        for (Musica m : musicas) {
            if (m.getYoutubeId() != null && m.getCapaUrl() != null && !m.getCapaUrl().isBlank()) {
                musicCovers.put(m.getYoutubeId(), m.getCapaUrl());
            }
        }

        Map<String, Object> modelo = new HashMap<>();
        modelo.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        modelo.put("musicas", musicas);
        modelo.put("totalMusicas", musicas.size());
        modelo.put("semMusicas", musicas.isEmpty() && (busca == null || busca.trim().isEmpty()));
        modelo.put("playlists", playlists);
        modelo.put("busca", busca != null ? busca : "");
        modelo.put("buscaAtiva", busca != null && !busca.trim().isEmpty());
        ctx.render("musicas/todas", modelo);
    }

    public void exibirCadastro(Context ctx) {
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        List<Album> albums = albumService.findAll(usuarioId);
        List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = obterPlaylistsVisiveis(usuarioId);
        renderizarFormulario(
                ctx,
                MusicaDTOView.cadastro(new MusicaDTO("", "", null, null, null, null, null, null, null, null), albums,
                        playlists),
                null);
    }

    @io.javalin.openapi.OpenApi(
            summary = "Catalogar nova música",
            operationId = "cadastrarMusica",
            path = "/musicas",
            methods = io.javalin.openapi.HttpMethod.POST,
            tags = {"Músicas"},
            responses = {
                    @io.javalin.openapi.OpenApiResponse(status = "303", description = "Redireciona para a home"),
                    @io.javalin.openapi.OpenApiResponse(status = "422", description = "Dados inválidos")
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
            String videoId = null;
            if (dto.youtubeUrl() != null && !dto.youtubeUrl().isBlank()) {
                videoId = playlistService.extractVideoId(dto.youtubeUrl()); // throws if invalid
                duracaoVal = playlistService.fetchYouTubeDuration(dto.youtubeUrl());
            }

            // Tenta criar a nova playlist primeiro. Se falhar, lança IllegalArgumentException e barra todo o processo.
            String novaPlaylistId = null;
            if (dto.novaPlaylistNome() != null && !dto.novaPlaylistNome().isBlank()) {
                novaPlaylistId = playlistService.createPlaylist(dto.novaPlaylistNome(), usuarioId).getId();
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
                    usuarioId,
                    videoId != null ? videoId : dto.youtubeId(),
                    dto.capaUrl());
            service.save(musica);

            // Publica na comunidade se solicitado pelo toggle
            String compartilhar = ctx.formParam("compartilharNaComunidade");
            if (comunidadeService != null && ("on".equals(compartilhar) || "true".equals(compartilhar))) {
                String opiniao = dto.resenha();
                if (opiniao == null) {
                    opiniao = "";
                }
                comunidadeService.criarPostCompartilhado(usuarioId, "SHARE_MUSIC", musica.getId(), opiniao);
            }

            if (dto.youtubeUrl() != null && !dto.youtubeUrl().isBlank()) {
                String redirectPlaylistId = null;
                String hiddenPlaylistId = playlistService.findOrCreateHiddenPlaylist(usuarioId).getId();

                try {
                    if (novaPlaylistId != null) {
                        playlistService.addItem(novaPlaylistId, dto.youtubeUrl(), videoId, usuarioId, dto.titulo());
                        redirectPlaylistId = novaPlaylistId;
                    } else if (dto.playlistId() != null && !dto.playlistId().isBlank() && !"NEW_TEMP".equals(dto.playlistId()) && !dto.playlistId().equals(hiddenPlaylistId)) {
                        playlistService.addItem(dto.playlistId(), dto.youtubeUrl(), videoId, usuarioId, dto.titulo());
                        redirectPlaylistId = dto.playlistId();
                    }
                } catch (Exception ignored) {
                    LOGGER.warn("Nao foi possivel adicionar musica na playlist visivel: {}", ignored.getMessage());
                }
                
                try {
                    playlistService.addItem(hiddenPlaylistId, dto.youtubeUrl(), videoId, usuarioId, dto.titulo());
                } catch (Exception ignored) {
                    LOGGER.warn("Nao foi possivel adicionar musica na playlist oculta: {}", ignored.getMessage());
                }
                
                if (redirectPlaylistId != null) {
                    ctx.redirect("/playlists/" + redirectPlaylistId, HttpStatus.SEE_OTHER);
                    return;
                }
            }

            redirecionarParaListagem(ctx);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("DTO de cadastro de música inválido. path={} mensagem={}", ctx.path(), e.getMessage());
            ctx.status(HttpStatus.UNPROCESSABLE_CONTENT);
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            List<Album> albums = albumService.findAll(usuarioId);
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = obterPlaylistsVisiveis(usuarioId);
            renderizarFormulario(ctx, MusicaDTOView.cadastro(dto, albums, playlists), e.getMessage());
        } catch (DatabaseException e) {
            LOGGER.error("Erro de banco de dados ao cadastrar música. path={}", ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            List<Album> albums = albumService.findAll(usuarioId);
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = obterPlaylistsVisiveis(usuarioId);
            renderizarFormulario(ctx, MusicaDTOView.cadastro(dto, albums, playlists),
                    "Erro ao salvar a música. Tente novamente.");
        } catch (Exception e) {
            LOGGER.error("Erro inesperado ao cadastrar música. path={}", ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            List<Album> albums = albumService.findAll(usuarioId);
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = obterPlaylistsVisiveis(usuarioId);
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
        List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = obterPlaylistsVisiveis(usuarioId);
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
                    usuarioId,
                    dto.youtubeId() != null ? dto.youtubeId() : musicaExistente.getYoutubeId(),
                    dto.capaUrl() != null ? dto.capaUrl() : musicaExistente.getCapaUrl());
            service.update(id, musicaDetails);

            // Publica na comunidade se solicitado pelo toggle
            String compartilhar = ctx.formParam("compartilharNaComunidade");
            if (comunidadeService != null && ("on".equals(compartilhar) || "true".equals(compartilhar))) {
                String opiniao = dto.resenha();
                if (opiniao == null) {
                    opiniao = "";
                }
                comunidadeService.criarPostCompartilhado(usuarioId, "SHARE_MUSIC", id, opiniao);
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
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            List<Album> albums = albumService.findAll(usuarioId);
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = obterPlaylistsVisiveis(usuarioId);
            renderizarFormulario(ctx, MusicaDTOView.edicao(id, dto, albums, playlists), e.getMessage());
        } catch (DatabaseException e) {
            LOGGER.error("Erro de banco de dados ao atualizar música. path={}", ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            List<Album> albums = albumService.findAll(usuarioId);
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = obterPlaylistsVisiveis(usuarioId);
            renderizarFormulario(ctx, MusicaDTOView.edicao(id, dto, albums, playlists),
                    "Erro ao salvar as alterações. Tente novamente.");
        } catch (Exception e) {
            LOGGER.error("Erro inesperado ao atualizar música. path={}", ctx.path(), e);
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR);
            String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
            List<Album> albums = albumService.findAll(usuarioId);
            List<br.ufpb.dcx.projetos.playlist.models.Playlist> playlists = obterPlaylistsVisiveis(usuarioId);
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
            ctx.status(HttpStatus.FORBIDDEN).result("Você não tem permissão para excluir esta música.");
            return;
        }

        // Exclusão em cascata: remove a música de todas as playlists do usuário
        if (playlistService != null) {
            playlistService.removerMusicaDeTodasPlaylists(musicaExistente.getTitulo(), musicaExistente.getArtista(), musicaExistente.getYoutubeId(), usuarioId);
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
                ctx.formParam("novaPlaylistNome"),
                ctx.formParam("youtubeId"),
                ctx.formParam("capaUrl")).normalizado();
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

    public void atualizarYoutubeId(Context ctx) {
        String id = ctx.pathParam("id");
        String usuarioId = ctx.sessionAttribute("usuarioLogadoId");
        
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> body = ctx.bodyAsClass(HashMap.class);
            String youtubeId = body.get("youtubeId");
            if (youtubeId != null && !youtubeId.isBlank()) {
                service.findById(id).filter(m -> m.getUsuarioId().equals(usuarioId)).ifPresent(m -> {
                    br.ufpb.dcx.projetos.musica.models.Musica atualizada = new br.ufpb.dcx.projetos.musica.models.Musica(
                        m.getId(),
                        m.getTitulo(),
                        m.getArtista(),
                        m.getGenero(),
                        m.getDuracaoSegundos(),
                        m.getResenha(),
                        m.getNota(),
                        m.getSpotifyUrl(),
                        m.getYoutubeUrl(),
                        m.getAlbumId(),
                        m.getUsuarioId(),
                        youtubeId,
                        m.getCapaUrl(),
                        m.isOcultaDaBiblioteca()
                    );
                    service.save(atualizada);
                });
                ctx.status(HttpStatus.OK).json(Map.of("status", "success"));
            } else {
                ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "youtubeId required"));
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of("error", e.getMessage()));
        }
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
                if (path != null && (path.equals("/musicas/todas") || path.equals("/musicas") || path.equals("/"))) {
                    String query = uri.getQuery();
                    String finalTarget = path + (query != null && !query.isBlank() ? "?" + query : "");
                    ctx.redirect(finalTarget, HttpStatus.SEE_OTHER);
                    return;
                }
            } catch (Exception ignored) {}
        }
        ctx.redirect("/musicas", HttpStatus.SEE_OTHER);
    }

    private List<br.ufpb.dcx.projetos.playlist.models.Playlist> obterPlaylistsVisiveis(String usuarioId) {
        if (playlistService == null || usuarioId == null) {
            return List.of();
        }
        return playlistService.findByUsuarioId(usuarioId).stream()
                .filter(p -> !p.isOculta() && !"Músicas Catalogadas".equalsIgnoreCase(p.getNome()) && !"Músicas do seu catálogo".equalsIgnoreCase(p.getNome()))
                .toList();
    }
}
