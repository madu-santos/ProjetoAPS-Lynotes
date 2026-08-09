package br.ufpb.dcx.projetos.rankings;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;

import java.util.*;
import java.util.stream.Collectors;

public class RankingArtistasStrategy implements RankingStrategy {

    private final ArtistaService artistaService;
    private final MusicaService musicaService;
    private final AlbumService albumService;

    public RankingArtistasStrategy(ArtistaService artistaService, MusicaService musicaService, AlbumService albumService) {
        this.artistaService = artistaService;
        this.musicaService = musicaService;
        this.albumService = albumService;
    }

    public RankingArtistasStrategy(ArtistaService artistaService, MusicaService musicaService) {
        this(artistaService, musicaService, null);
    }

    @Override
    public List<RankingItemDTO> calcularRanking() {
        List<Artista> artistas = artistaService != null ? artistaService.findAllGlobal() : Collections.emptyList();
        List<Musica> musicas = musicaService != null ? musicaService.findAll() : Collections.emptyList();
        List<Album> albuns = albumService != null ? albumService.findAllGlobal() : Collections.emptyList();

        Map<String, Set<String>> ouvintesPorArtista = new HashMap<>();

        for (Artista a : artistas) {
            if (a.getNome() != null && !a.getNome().isBlank() && a.getUsuarioId() != null) {
                String key = a.getNome().trim().toLowerCase();
                if (isNomeArtistaValido(key)) {
                    ouvintesPorArtista.computeIfAbsent(key, k -> new HashSet<>()).add(a.getUsuarioId());
                }
            }
        }

        for (Musica m : musicas) {
            if (m.getArtista() != null && !m.getArtista().isBlank() && m.getUsuarioId() != null) {
                String key = m.getArtista().trim().toLowerCase();
                if (isNomeArtistaValido(key)) {
                    ouvintesPorArtista.computeIfAbsent(key, k -> new HashSet<>()).add(m.getUsuarioId());
                }
            }
        }

        for (Album alb : albuns) {
            if (alb.getArtista() != null && !alb.getArtista().isBlank() && alb.getUsuarioId() != null) {
                String key = alb.getArtista().trim().toLowerCase();
                if (isNomeArtistaValido(key)) {
                    ouvintesPorArtista.computeIfAbsent(key, k -> new HashSet<>()).add(alb.getUsuarioId());
                }
            }
        }

        Map<String, Artista> artistaPorNome = artistas.stream()
                .filter(a -> a.getNome() != null && !a.getNome().isBlank())
                .collect(Collectors.toMap(
                        a -> a.getNome().trim().toLowerCase(),
                        a -> a,
                        (a1, a2) -> a1
                ));

        List<Map.Entry<String, Set<String>>> ordenados = ouvintesPorArtista.entrySet().stream()
                .filter(e -> isNomeArtistaValido(e.getKey()))
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .toList();

        List<RankingItemDTO> ranking = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Set<String>> entry : ordenados) {
            String nomeMinusculo = entry.getKey();
            int totalOuvintes = entry.getValue().size();

            Artista art = artistaPorNome.get(nomeMinusculo);
            String nomeExibicao = art != null ? art.getNome() : formatarNome(nomeMinusculo);
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

            if (capaUrl == null || capaUrl.isBlank()) {
                try {
                    capaUrl = "https://ui-avatars.com/api/?name=" + java.net.URLEncoder.encode(nomeExibicao, java.nio.charset.StandardCharsets.UTF_8) + "&background=b268ff&color=ffffff&size=300";
                } catch (Exception ignored) {}
            }

            String subtitulo = totalOuvintes + (totalOuvintes == 1 ? " ouvinte" : " ouvintes");

            ranking.add(new RankingItemDTO(
                    id,
                    nomeExibicao,
                    subtitulo,
                    (double) totalOuvintes,
                    totalOuvintes,
                    "ARTISTA",
                    "Top #" + rank,
                    capaUrl
            ));
            rank++;
        }

        return ranking;
    }

    private boolean isNomeArtistaValido(String name) {
        if (name == null || name.isBlank()) return false;
        String lower = name.trim().toLowerCase();
        if (lower.contains("- topic") || lower.contains("topic -") || lower.endsWith(" topic") || lower.startsWith("topic ") ||
            lower.contains("- tema") || lower.contains("tema -") || lower.endsWith(" tema") || lower.startsWith("tema ") ||
            lower.contains("tópico") || lower.contains("vevo") || lower.contains("full album") ||
            lower.contains("- single") || lower.endsWith(" single") || lower.contains("http") ||
            lower.contains("www.") || lower.contains("results?") || lower.contains("official video") ||
            lower.contains("clipe oficial") || lower.contains("lyric video") || lower.contains("audiotrack") ||
            lower.contains("track_") || lower.contains("itunes_") || lower.contains("official audio") ||
            lower.contains("clipe") || lower.contains("áudio oficial") || lower.contains("audio oficial") ||
            lower.contains("tema de ") || lower.endsWith("tema")) {
            return false;
        }
        return true;
    }

    private String formatarNome(String nome) {
        if (nome == null || nome.isEmpty()) return "";
        return Arrays.stream(nome.split("\\s+"))
                .map(w -> !w.isEmpty() ? Character.toUpperCase(w.charAt(0)) + w.substring(1) : "")
                .collect(Collectors.joining(" "));
    }
}

