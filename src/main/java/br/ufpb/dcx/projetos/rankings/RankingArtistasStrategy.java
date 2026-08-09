package br.ufpb.dcx.projetos.rankings;

import br.ufpb.dcx.projetos.album.models.Album;
import br.ufpb.dcx.projetos.album.services.AlbumService;
import br.ufpb.dcx.projetos.artista.models.Artista;
import br.ufpb.dcx.projetos.artista.services.ArtistaService;
import br.ufpb.dcx.projetos.musica.models.Musica;
import br.ufpb.dcx.projetos.musica.services.MusicaService;

import java.util.*;

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

        // Filtra apenas perfis de artistas válidos (eliminando tópicos do YouTube e canais não musicais)
        List<Artista> perfisArtistasValidos = artistas.stream()
                .filter(a -> a.getNome() != null && !a.getNome().isBlank())
                .filter(a -> isNomeArtistaValido(a.getNome()))
                .toList();

        if (perfisArtistasValidos.isEmpty()) {
            return Collections.emptyList();
        }

        // Mapeia artistas por chave normalizada (ex: "lady gaga" -> Artista)
        Map<String, Artista> artistaPorNome = new HashMap<>();
        Map<String, Set<String>> ouvintesPorArtista = new HashMap<>();

        for (Artista a : perfisArtistasValidos) {
            String cleanName = limparNomeArtista(a.getNome());
            if (!isNomeArtistaValido(cleanName)) {
                continue;
            }
            String key = cleanName.toLowerCase();
            artistaPorNome.putIfAbsent(key, a);
            ouvintesPorArtista.computeIfAbsent(key, k -> new HashSet<>());
            if (a.getUsuarioId() != null) {
                ouvintesPorArtista.get(key).add(a.getUsuarioId());
            }
        }

        // Contabiliza ouvintes das músicas cadastradas associadas aos perfis de artistas válidos
        for (Musica m : musicas) {
            if (m.getArtista() != null && !m.getArtista().isBlank() && m.getUsuarioId() != null) {
                String cleanMusicArtist = limparNomeArtista(m.getArtista()).toLowerCase();
                if (ouvintesPorArtista.containsKey(cleanMusicArtist)) {
                    ouvintesPorArtista.get(cleanMusicArtist).add(m.getUsuarioId());
                }
            }
        }

        // Contabiliza ouvintes dos álbuns cadastrados associados aos perfis de artistas válidos
        for (Album alb : albuns) {
            if (alb.getArtista() != null && !alb.getArtista().isBlank() && alb.getUsuarioId() != null) {
                String cleanAlbumArtist = limparNomeArtista(alb.getArtista()).toLowerCase();
                if (ouvintesPorArtista.containsKey(cleanAlbumArtist)) {
                    ouvintesPorArtista.get(cleanAlbumArtist).add(alb.getUsuarioId());
                }
            }
        }

        // Ordena por quantidade de ouvintes decrescente
        List<Map.Entry<String, Set<String>>> ordenados = ouvintesPorArtista.entrySet().stream()
                .sorted((e1, e2) -> Integer.compare(e2.getValue().size(), e1.getValue().size()))
                .toList();

        List<RankingItemDTO> ranking = new ArrayList<>();
        int rank = 1;
        for (Map.Entry<String, Set<String>> entry : ordenados) {
            String nomeMinusculo = entry.getKey();
            Artista art = artistaPorNome.get(nomeMinusculo);
            if (art == null) {
                continue;
            }

            int totalOuvintes = entry.getValue().size();
            String nomeExibicao = art.getNome();
            String capaUrl = art.getCapaUrl();

            if (capaUrl == null || capaUrl.isBlank()) {
                capaUrl = musicas.stream()
                        .filter(m -> m.getArtista() != null && limparNomeArtista(m.getArtista()).equalsIgnoreCase(nomeMinusculo))
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
                    art.getId(),
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

    public static String limparNomeArtista(String nome) {
        if (nome == null || nome.isBlank()) return "";
        String limpo = nome.trim();
        limpo = limpo.replaceAll("(?i)\\s*-\\s*(topic|tópico|tema)\\b", "");
        limpo = limpo.replaceAll("(?i)\\s+(topic|tópico|tema)$", "");
        limpo = limpo.replaceAll("(?i)\\s*-\\s*vevo\\b", "");
        limpo = limpo.replaceAll("(?i)\\s+vevo$", "");
        return limpo.trim();
    }

    private boolean isNomeArtistaValido(String name) {
        if (name == null || name.isBlank()) return false;
        String lower = name.trim().toLowerCase();
        if (lower.equals("topic") || lower.equals("tópico") || lower.equals("tema") || lower.equals("vevo") ||
            lower.contains("- topic") || lower.contains("topic -") || lower.endsWith(" topic") || lower.startsWith("topic ") ||
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
}
