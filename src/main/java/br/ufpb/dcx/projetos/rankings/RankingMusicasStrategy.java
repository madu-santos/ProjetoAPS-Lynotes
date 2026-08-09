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
            if (totalAvaliacoes <= 0) {
                continue;
            }
            double media = notas.stream().mapToDouble(Double::doubleValue).sum() / totalAvaliacoes;
            String mediaStr = String.format(Locale.US, "%.1f", media);

            // Pontuação ponderada: prioriza mais avaliações com boas notas
            double rankingScore = (media * (1.0 - (1.0 / (totalAvaliacoes + 1.0))) + (totalSalvos * 0.01));

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

        ranking.sort((a, b) -> {
            double valA = a.notaFormatada() != null ? Double.parseDouble(a.notaFormatada()) : 0.0;
            double valB = b.notaFormatada() != null ? Double.parseDouble(b.notaFormatada()) : 0.0;
            int cmp = Double.compare(valB, valA); // Descendente
            if (cmp != 0) return cmp;
            
            int cmpCount = Integer.compare(b.totalAvaliacoes(), a.totalAvaliacoes()); // Descendente
            if (cmpCount != 0) return cmpCount;
            
            return a.titulo().compareToIgnoreCase(b.titulo()); // Ascendente
        });

        return ranking;
    }
}

