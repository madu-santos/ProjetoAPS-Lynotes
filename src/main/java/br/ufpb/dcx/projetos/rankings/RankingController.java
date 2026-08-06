package br.ufpb.dcx.projetos.rankings;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;
import io.javalin.http.Context;

import java.util.*;
import java.util.stream.Collectors;

public class RankingController {

    private final MusicaService musicaService;
    private final AlbumService albumService;
    private final ArtistaService artistaService;

    private static List<RankingItemDTO> cachedMusicas = null;
    private static List<RankingItemDTO> cachedAlbuns = null;
    private static List<RankingItemDTO> cachedArtistas = null;
    private static java.time.LocalDateTime lastCacheUpdate = null;

    public RankingController(MusicaService musicaService, AlbumService albumService, ArtistaService artistaService) {
        this.musicaService = musicaService;
        this.albumService = albumService;
        this.artistaService = artistaService;
    }

    private synchronized void updateCacheIfNeeded() {
        if (lastCacheUpdate == null || lastCacheUpdate.isBefore(java.time.LocalDateTime.now().minusMinutes(1)) 
            || cachedMusicas == null || cachedAlbuns == null || cachedArtistas == null) {
            cachedMusicas = calcularRankingMusicas(musicaService.findAll());
            cachedAlbuns = calcularRankingAlbuns(albumService.findAllGlobal());
            cachedArtistas = calcularRankingArtistas(artistaService.findAllGlobal());
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
        
        model.put("topMusicas", getTopMusicas().stream().limit(10).collect(Collectors.toList()));
        model.put("topAlbuns", getTopAlbuns().stream().limit(10).collect(Collectors.toList()));
        model.put("topArtistas", getTopArtistas().stream().limit(10).collect(Collectors.toList()));

        ctx.render("rankings/index.html", model);
    }

    public List<RankingItemDTO> calcularRankingMusicas(List<Musica> musicas) {
        Map<String, List<Musica>> agrupado = musicas.stream()
                .filter(m -> m.getNota() != null && m.getTitulo() != null && m.getArtista() != null)
                .collect(Collectors.groupingBy(m -> (m.getTitulo().toLowerCase() + "|||" + m.getArtista().toLowerCase())));

        List<RankingItemDTO> ranking = new ArrayList<>();
        for (List<Musica> grupo : agrupado.values()) {
            Musica rep = grupo.get(0);
            double soma = grupo.stream().mapToDouble(Musica::getNota).sum();
            double media = soma / grupo.size();
            String mediaStr = String.format(Locale.US, "%.1f", media);
            ranking.add(new RankingItemDTO(rep.getYoutubeId() != null ? rep.getYoutubeId() : rep.getId(), rep.getTitulo(), rep.getArtista(), media, grupo.size(), "MUSICA", mediaStr));
        }
        ranking.sort(Comparator.comparing(RankingItemDTO::notaMedia).reversed().thenComparing(RankingItemDTO::totalAvaliacoes).reversed());
        return ranking;
    }

    public List<RankingItemDTO> calcularRankingAlbuns(List<Album> albuns) {
        Map<String, List<Album>> agrupado = albuns.stream()
                .filter(a -> a.getNota() != null && a.getTitulo() != null && a.getArtista() != null)
                .collect(Collectors.groupingBy(a -> (a.getTitulo().toLowerCase() + "|||" + a.getArtista().toLowerCase())));

        List<RankingItemDTO> ranking = new ArrayList<>();
        for (List<Album> grupo : agrupado.values()) {
            Album rep = grupo.get(0);
            double soma = grupo.stream().mapToDouble(Album::getNota).sum();
            double media = soma / grupo.size();
            String mediaStr = String.format(Locale.US, "%.1f", media);
            ranking.add(new RankingItemDTO(rep.getId(), rep.getTitulo(), rep.getArtista(), media, grupo.size(), "ALBUM", mediaStr));
        }
        ranking.sort(Comparator.comparing(RankingItemDTO::notaMedia).reversed().thenComparing(RankingItemDTO::totalAvaliacoes).reversed());
        return ranking;
    }

    public List<RankingItemDTO> calcularRankingArtistas(List<Artista> artistas) {
        // Agrupa artistas por nome (case-insensitive) e conta usuários distintos que salvaram
        Map<String, List<Artista>> agrupado = artistas.stream()
                .filter(a -> a.getNome() != null && !a.getNome().isBlank())
                .collect(Collectors.groupingBy(a -> a.getNome().toLowerCase()));

        List<RankingItemDTO> ranking = new ArrayList<>();
        for (List<Artista> grupo : agrupado.values()) {
            Artista rep = grupo.get(0);
            // Conta usuários distintos que salvaram este artista
            long totalUsuarios = grupo.stream()
                    .map(Artista::getUsuarioId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();
            String subtitulo = totalUsuarios + (totalUsuarios == 1 ? " usuário salvou" : " usuários salvaram");
            ranking.add(new RankingItemDTO(rep.getId(), rep.getNome(), subtitulo, (double) totalUsuarios, (int) totalUsuarios, "ARTISTA", String.valueOf(totalUsuarios)));
        }
        ranking.sort(Comparator.comparing(RankingItemDTO::notaMedia).reversed());
        return ranking;
    }
}
