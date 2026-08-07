package br.ufpb.dcx.projetos.rankings;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class RankingAlbunsStrategy implements RankingStrategy {

    private final AlbumService albumService;

    public RankingAlbunsStrategy(AlbumService albumService) {
        this.albumService = albumService;
    }

    @Override
    public List<RankingItemDTO> calcularRanking() {
        List<Album> albuns = albumService.findAllGlobal();
        Map<String, List<Album>> agrupado = albuns.stream()
                .filter(a -> a.getNota() != null && a.getTitulo() != null && a.getArtista() != null)
                .collect(Collectors.groupingBy(a -> a.getTitulo().toLowerCase() + "|||" + a.getArtista().toLowerCase()));

        List<RankingItemDTO> ranking = new ArrayList<>();
        for (List<Album> grupo : agrupado.values()) {
            Album rep = grupo.getFirst();
            double media = grupo.stream().mapToDouble(Album::getNota).sum() / grupo.size();
            String mediaStr = String.format(Locale.US, "%.1f", media);
            ranking.add(new RankingItemDTO(rep.getId(), rep.getTitulo(), rep.getArtista(), media, grupo.size(), "ALBUM", mediaStr, rep.getCapaUrl()));
        }
        ranking.sort(Comparator.comparing(RankingItemDTO::notaMedia).reversed().thenComparing(RankingItemDTO::totalAvaliacoes).reversed());
        return ranking;
    }
}

