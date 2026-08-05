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

    public RankingController(MusicaService musicaService, AlbumService albumService, ArtistaService artistaService) {
        this.musicaService = musicaService;
        this.albumService = albumService;
        this.artistaService = artistaService;
    }

    public void index(Context ctx) {
        List<Musica> todasMusicas = musicaService.findAll();
        List<Album> todosAlbuns = albumService.findAllGlobal();
        List<Artista> todosArtistas = artistaService.findAllGlobal();

        List<RankingItemDTO> rankingMusicas = calcularRankingMusicas(todasMusicas);
        List<RankingItemDTO> rankingAlbuns = calcularRankingAlbuns(todosAlbuns);
        List<RankingItemDTO> rankingArtistas = calcularRankingArtistas(todosArtistas);

        Map<String, Object> model = new HashMap<>();
        model.put("usuarioLogado", ctx.sessionAttribute("usuarioLogado"));
        model.put("paginaAtiva", "rankings");
        
        model.put("topMusicas", rankingMusicas.stream().limit(10).collect(Collectors.toList()));
        model.put("topAlbuns", rankingAlbuns.stream().limit(10).collect(Collectors.toList()));
        model.put("topArtistas", rankingArtistas.stream().limit(10).collect(Collectors.toList()));

        ctx.render("rankings/index.html", model);
    }

    private List<RankingItemDTO> calcularRankingMusicas(List<Musica> musicas) {
        Map<String, List<Musica>> agrupado = musicas.stream()
                .filter(m -> m.getNota() != null && m.getTitulo() != null && m.getArtista() != null)
                .collect(Collectors.groupingBy(m -> (m.getTitulo().toLowerCase() + "|||" + m.getArtista().toLowerCase())));

        List<RankingItemDTO> ranking = new ArrayList<>();
        for (List<Musica> grupo : agrupado.values()) {
            Musica rep = grupo.get(0);
            double soma = grupo.stream().mapToDouble(Musica::getNota).sum();
            double media = soma / grupo.size();
            String mediaStr = String.format(Locale.US, "%.1f", media);
            ranking.add(new RankingItemDTO(rep.getId(), rep.getTitulo(), rep.getArtista(), media, grupo.size(), "MUSICA", mediaStr));
        }
        ranking.sort(Comparator.comparing(RankingItemDTO::notaMedia).reversed().thenComparing(RankingItemDTO::totalAvaliacoes).reversed());
        return ranking;
    }

    private List<RankingItemDTO> calcularRankingAlbuns(List<Album> albuns) {
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

    private List<RankingItemDTO> calcularRankingArtistas(List<Artista> artistas) {
        Map<String, List<Artista>> agrupado = artistas.stream()
                .filter(a -> a.getNota() != null && a.getNome() != null)
                .collect(Collectors.groupingBy(a -> a.getNome().toLowerCase()));

        List<RankingItemDTO> ranking = new ArrayList<>();
        for (List<Artista> grupo : agrupado.values()) {
            Artista rep = grupo.get(0);
            double soma = grupo.stream().mapToDouble(Artista::getNota).sum();
            double media = soma / grupo.size();
            String mediaStr = String.format(Locale.US, "%.1f", media);
            ranking.add(new RankingItemDTO(rep.getId(), rep.getNome(), "Artista", media, grupo.size(), "ARTISTA", mediaStr));
        }
        ranking.sort(Comparator.comparing(RankingItemDTO::notaMedia).reversed().thenComparing(RankingItemDTO::totalAvaliacoes).reversed());
        return ranking;
    }
}
