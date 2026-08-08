package br.ufpb.dcx.projetos.rankings;

import br.ufpb.dcx.projetos.musica.services.MusicaService;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import io.javalin.http.Context;

import java.util.*;

public class RankingController {

    private final Map<String, RankingStrategy> estrategias;

    private static List<RankingItemDTO> cachedMusicas = null;
    private static List<RankingItemDTO> cachedAlbuns = null;
    private static List<RankingItemDTO> cachedArtistas = null;
    private static java.time.LocalDateTime lastCacheUpdate = null;

    public RankingController(MusicaService musicaService, AlbumService albumService, ArtistaService artistaService) {
        this.estrategias = Map.of(
                "musicas", new RankingMusicasStrategy(musicaService),
                "albuns", new RankingAlbunsStrategy(albumService),
                "artistas", new RankingArtistasStrategy(artistaService, musicaService, albumService)
        );
    }

    private synchronized void updateCacheIfNeeded() {
        if (lastCacheUpdate == null || lastCacheUpdate.isBefore(java.time.LocalDateTime.now().minusSeconds(5)) 
            || cachedMusicas == null || cachedAlbuns == null || cachedArtistas == null) {
            cachedMusicas = estrategias.get("musicas").calcularRanking();
            cachedAlbuns = estrategias.get("albuns").calcularRanking();
            cachedArtistas = estrategias.get("artistas").calcularRanking();
            lastCacheUpdate = java.time.LocalDateTime.now();
        }
    }

    public List<RankingItemDTO> getTopMusicas() {
        updateCacheIfNeeded();
        return cachedMusicas;
    }

    public List<RankingItemDTO> getTopAlbuns() {
        updateCacheIfNeeded();
        return cachedAlbuns;
    }

    public List<RankingItemDTO> getTopArtistas() {
        updateCacheIfNeeded();
        return cachedArtistas;
    }

    public void index(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        model.put("paginaAtiva", "rankings");
        
        model.put("topMusicas", getTopMusicas().stream().limit(10).toList());
        model.put("topAlbuns", getTopAlbuns().stream().limit(10).toList());
        model.put("topArtistas", getTopArtistas().stream().limit(10).toList());

        ctx.render("rankings/index.html", model);
    }
}
