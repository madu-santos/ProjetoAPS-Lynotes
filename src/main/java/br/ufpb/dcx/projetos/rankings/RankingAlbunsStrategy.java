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
            double media = totalAvaliacoes > 0 ? (notas.stream().mapToDouble(Double::doubleValue).sum() / totalAvaliacoes) : 0.0;
            String mediaStr = totalAvaliacoes > 0 ? String.format(Locale.US, "%.1f", media) : null;

            // Pontuação ponderada: prioriza mais avaliações com notas altas (ex: 4.5 por 2 usuários vence 5.0 por 1 usuário)
            double rankingScore = totalAvaliacoes > 0
                    ? (media * (1.0 - (1.0 / (totalAvaliacoes + 1.0))) + (totalSalvos * 0.01))
                    : (totalSalvos * 0.01);

            String subtitulo = rep.getArtista();

            ranking.add(new RankingItemDTO(
                    rep.getId(),
                    rep.getTitulo(),
                    subtitulo,
                    rankingScore,
                    totalAvaliacoes > 0 ? totalAvaliacoes : (int) totalSalvos,
                    "ALBUM",
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

