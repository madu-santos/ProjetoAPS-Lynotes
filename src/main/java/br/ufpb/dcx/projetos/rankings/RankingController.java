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
            List<Musica> allMusicas = musicaService.findAll();
            cachedMusicas = calcularRankingMusicas(allMusicas);
            cachedAlbuns = calcularRankingAlbuns(albumService.findAllGlobal());
            cachedArtistas = calcularRankingArtistas(artistaService.findAllGlobal(), allMusicas);
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
            ranking.add(new RankingItemDTO(rep.getYoutubeId() != null ? rep.getYoutubeId() : rep.getId(), rep.getTitulo(), rep.getArtista(), media, grupo.size(), "MUSICA", mediaStr, rep.getCapaUrl()));
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
            ranking.add(new RankingItemDTO(rep.getId(), rep.getTitulo(), rep.getArtista(), media, grupo.size(), "ALBUM", mediaStr, rep.getCapaUrl()));
        }
        ranking.sort(Comparator.comparing(RankingItemDTO::notaMedia).reversed().thenComparing(RankingItemDTO::totalAvaliacoes).reversed());
        return ranking;
    }

    public List<RankingItemDTO> calcularRankingArtistas(List<Artista> artistas, List<Musica> musicas) {
        // Agrupa todas as músicas pelo nome do artista (case-insensitive) e conta a frequência
        Map<String, Long> frequenciaArtista = musicas.stream()
                .filter(m -> m.getArtista() != null && !m.getArtista().isBlank())
                .collect(Collectors.groupingBy(m -> m.getArtista().trim().toLowerCase(), Collectors.counting()));

        // Mapeia o nome do artista (case-insensitive) para o objeto Artista para obter a capaUrl e nome correto
        Map<String, Artista> artistaPorNome = artistas.stream()
                .filter(a -> a.getNome() != null && !a.getNome().isBlank())
                .collect(Collectors.toMap(
                        a -> a.getNome().trim().toLowerCase(),
                        a -> a,
                        (a1, a2) -> a1 // mantém o primeiro em caso de duplicatas
                ));

        List<RankingItemDTO> ranking = new ArrayList<>();
        
        // Vamos ordenar os artistas por frequência decrescente
        List<Map.Entry<String, Long>> ordenados = frequenciaArtista.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());

        int rank = 1;
        for (Map.Entry<String, Long> entry : ordenados) {
            String nomeMinusculo = entry.getKey();
            long totalOuvidos = entry.getValue();
            
            // Busca o objeto Artista correspondente
            Artista art = artistaPorNome.get(nomeMinusculo);
            String nomeExibicao = art != null ? art.getNome() : formatarNome(entry.getKey());
            String capaUrl = art != null ? art.getCapaUrl() : null;
            String id = art != null ? art.getId() : UUID.randomUUID().toString();

            // Se não tiver capa do artista, tenta pegar a capa de alguma música dele
            if (capaUrl == null || capaUrl.isBlank()) {
                capaUrl = musicas.stream()
                        .filter(m -> m.getArtista() != null && m.getArtista().trim().toLowerCase().equals(nomeMinusculo))
                        .map(Musica::getCapaUrl)
                        .filter(url -> url != null && !url.isBlank())
                        .findFirst()
                        .orElse(null);
            }

            String subtitulo = "Top #" + rank;
            ranking.add(new RankingItemDTO(id, nomeExibicao, subtitulo, (double) totalOuvidos, (int) totalOuvidos, "ARTISTA", String.valueOf(totalOuvidos), capaUrl));
            rank++;
        }

        return ranking;
    }

    private String formatarNome(String nome) {
        if (nome == null || nome.isEmpty()) return "";
        return Arrays.stream(nome.split("\\s+"))
                .map(w -> w.length() > 0 ? Character.toUpperCase(w.charAt(0)) + w.substring(1) : "")
                .collect(Collectors.joining(" "));
    }
}
