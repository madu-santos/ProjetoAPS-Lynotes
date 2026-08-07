package br.ufpb.dcx.projetos.rankings;

import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class RankingMusicasStrategy implements RankingStrategy {

    private final MusicaService musicaService;

    public RankingMusicasStrategy(MusicaService musicaService) {
        this.musicaService = musicaService;
    }

    @Override
    public List<RankingItemDTO> calcularRanking() {
        List<Musica> musicas = musicaService.findAll();
        Map<String, List<Musica>> agrupado = musicas.stream()
                .filter(m -> m.getNota() != null && m.getTitulo() != null && m.getArtista() != null)
                .collect(Collectors.groupingBy(m -> m.getTitulo().toLowerCase() + "|||" + m.getArtista().toLowerCase()));

        List<RankingItemDTO> ranking = new ArrayList<>();
        for (List<Musica> grupo : agrupado.values()) {
            Musica rep = grupo.getFirst();
            double media = grupo.stream().mapToDouble(Musica::getNota).sum() / grupo.size();
            String mediaStr = String.format(Locale.US, "%.1f", media);
            ranking.add(new RankingItemDTO(rep.getYoutubeId() != null ? rep.getYoutubeId() : rep.getId(), rep.getTitulo(), rep.getArtista(), media, grupo.size(), "MUSICA", mediaStr, rep.getCapaUrl()));
        }
        ranking.sort(Comparator.comparing(RankingItemDTO::notaMedia).reversed().thenComparing(RankingItemDTO::totalAvaliacoes).reversed());
        return ranking;
    }
}

