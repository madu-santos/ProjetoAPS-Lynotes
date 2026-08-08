package br.ufpb.dcx.projetos.rankings;

import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;

import java.util.*;
import java.util.stream.Collectors;

public class RankingMusicasStrategy implements RankingStrategy {

    private final MusicaService musicaService;

    public RankingMusicasStrategy(MusicaService musicaService) {
        this.musicaService = musicaService;
    }

    @Override
    public List<RankingItemDTO> calcularRanking() {
        List<Musica> musicas = musicaService.findAll();
        if (musicas == null || musicas.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Musica>> agrupado = musicas.stream()
                .filter(m -> m.getTitulo() != null && !m.getTitulo().isBlank() && m.getArtista() != null && !m.getArtista().isBlank())
                .collect(Collectors.groupingBy(m -> {
                    if (m.getYoutubeId() != null && !m.getYoutubeId().isBlank()) {
                        return "yt:" + m.getYoutubeId().trim().toLowerCase();
                    }
                    return m.getTitulo().trim().toLowerCase() + "|||" + m.getArtista().trim().toLowerCase();
                }));

        List<RankingItemDTO> ranking = new ArrayList<>();
        for (List<Musica> grupo : agrupado.values()) {
            Musica rep = grupo.getFirst();

            String capaUrl = grupo.stream()
                    .map(Musica::getCapaUrl)
                    .filter(c -> c != null && !c.isBlank())
                    .findFirst()
                    .orElse(null);

            List<Double> notas = grupo.stream()
                    .map(Musica::getNota)
                    .filter(Objects::nonNull)
                    .filter(n -> n > 0)
                    .toList();

            long totalSalvos = grupo.stream()
                    .map(Musica::getUsuarioId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            int totalAvaliacoes = notas.size();
            double media = totalAvaliacoes > 0 ? (notas.stream().mapToDouble(Double::doubleValue).sum() / totalAvaliacoes) : 0.0;
            String mediaStr = totalAvaliacoes > 0 ? String.format(Locale.US, "%.1f", media) : null;

            // Pontuação ponderada: prioriza mais avaliações com boas notas
            double rankingScore = totalAvaliacoes > 0
                    ? (media * (1.0 - (1.0 / (totalAvaliacoes + 1.0))) + (totalSalvos * 0.01))
                    : (totalSalvos * 0.01);

            String subtitulo = rep.getArtista();
            String id = rep.getYoutubeId() != null && !rep.getYoutubeId().isBlank() ? rep.getYoutubeId() : rep.getId();

            ranking.add(new RankingItemDTO(
                    id,
                    rep.getTitulo(),
                    subtitulo,
                    rankingScore,
                    totalAvaliacoes > 0 ? totalAvaliacoes : (int) totalSalvos,
                    "MUSICA",
                    mediaStr,
                    capaUrl
            ));
        }

        ranking.sort(Comparator.comparing(RankingItemDTO::notaMedia).reversed()
                .thenComparing(RankingItemDTO::totalAvaliacoes).reversed()
                .thenComparing(RankingItemDTO::titulo));

        return ranking;
    }
}

