package br.ufpb.dcx.projetos.rankings;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;

import java.util.*;
import java.util.stream.Collectors;

public class RankingAlbunsStrategy implements RankingStrategy {

    private final AlbumService albumService;

    public RankingAlbunsStrategy(AlbumService albumService) {
        this.albumService = albumService;
    }

    @Override
    public List<RankingItemDTO> calcularRanking() {
        List<Album> albuns = albumService.findAllGlobal();
        if (albuns == null || albuns.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<Album>> agrupado = albuns.stream()
                .filter(a -> a.getTitulo() != null && !a.getTitulo().isBlank() && a.getArtista() != null && !a.getArtista().isBlank())
                .collect(Collectors.groupingBy(a -> a.getTitulo().trim().toLowerCase() + "|||" + a.getArtista().trim().toLowerCase()));

        List<RankingItemDTO> ranking = new ArrayList<>();
        for (List<Album> grupo : agrupado.values()) {
            Album rep = grupo.getFirst();

            String capaUrl = grupo.stream()
                    .map(Album::getCapaUrl)
                    .filter(c -> c != null && !c.isBlank())
                    .findFirst()
                    .orElse(null);

            List<Double> notas = grupo.stream()
                    .map(Album::getNota)
                    .filter(Objects::nonNull)
                    .filter(n -> n > 0)
                    .toList();

            long totalSalvos = grupo.stream()
                    .map(Album::getUsuarioId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .count();

            int totalAvaliacoes = notas.size();
            if (totalAvaliacoes <= 0) {
                continue;
            }
            double media = notas.stream().mapToDouble(Double::doubleValue).sum() / totalAvaliacoes;
            String mediaStr = String.format(Locale.US, "%.1f", media);

            // Pontuação ponderada: prioriza mais avaliações com notas altas
            double rankingScore = (media * (1.0 - (1.0 / (totalAvaliacoes + 1.0))) + (totalSalvos * 0.01));

            String subtitulo = rep.getArtista();

            ranking.add(new RankingItemDTO(
                    rep.getId(),
                    rep.getTitulo(),
                    subtitulo,
                    rankingScore,
                    totalAvaliacoes,
                    "ALBUM",
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

