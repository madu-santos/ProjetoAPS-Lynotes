package br.ufpb.dcx.projetos.rankings;

import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public class RankingArtistasStrategy implements RankingStrategy {

    private final ArtistaService artistaService;
    private final MusicaService musicaService;

    public RankingArtistasStrategy(ArtistaService artistaService, MusicaService musicaService) {
        this.artistaService = artistaService;
        this.musicaService = musicaService;
    }

    @Override
    public List<RankingItemDTO> calcularRanking() {
        List<Artista> artistas = artistaService.findAllGlobal();
        List<Musica> musicas = musicaService.findAll();

        Map<String, Long> frequenciaArtista = musicas.stream()
                .filter(m -> m.getArtista() != null && !m.getArtista().isBlank())
                .collect(Collectors.groupingBy(m -> m.getArtista().trim().toLowerCase(), Collectors.counting()));

        Map<String, Artista> artistaPorNome = artistas.stream()
                .filter(a -> a.getNome() != null && !a.getNome().isBlank())
                .collect(Collectors.toMap(
                        a -> a.getNome().trim().toLowerCase(),
                        a -> a,
                        (a1, a2) -> a1
                ));

        List<Map.Entry<String, Long>> ordenados = frequenciaArtista.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        int rank = 1;
        List<RankingItemDTO> ranking = new java.util.ArrayList<>();
        for (Map.Entry<String, Long> entry : ordenados) {
            String nomeMinusculo = entry.getKey();
            long totalOuvidos = entry.getValue();
            Artista art = artistaPorNome.get(nomeMinusculo);
            String nomeExibicao = art != null ? art.getNome() : formatarNome(entry.getKey());
            String capaUrl = art != null ? art.getCapaUrl() : null;
            String id = art != null ? art.getId() : UUID.randomUUID().toString();

            if (capaUrl == null || capaUrl.isBlank()) {
                capaUrl = musicas.stream()
                        .filter(m -> m.getArtista() != null && m.getArtista().trim().toLowerCase().equals(nomeMinusculo))
                        .map(Musica::getCapaUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .findFirst()
                        .orElse(null);
            }

            ranking.add(new RankingItemDTO(id, nomeExibicao, "Top #" + rank, (double) totalOuvidos, (int) totalOuvidos, "ARTISTA", String.valueOf(totalOuvidos), capaUrl));
            rank++;
        }

        return ranking;
    }

    private String formatarNome(String nome) {
        if (nome == null || nome.isEmpty()) return "";
        return Arrays.stream(nome.split("\\s+"))
                .map(w -> !w.isEmpty() ? Character.toUpperCase(w.charAt(0)) + w.substring(1) : "")
                .collect(Collectors.joining(" "));
    }
}

