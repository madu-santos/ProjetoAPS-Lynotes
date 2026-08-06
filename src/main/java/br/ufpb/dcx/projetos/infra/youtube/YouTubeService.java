package br.ufpb.dcx.projetos.infra.youtube;

import br.ufpb.dcx.projetos.infra.youtube.dto.YouTubeItemDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class YouTubeService {

    private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeService.class);
    private static final String YOUTUBE_SEARCH_URL = "https://www.googleapis.com/youtube/v3/search";
    private static final String YOUTUBE_VIDEOS_URL = "https://www.googleapis.com/youtube/v3/videos";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public YouTubeService() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(6)).build(), new ObjectMapper());
    }

    public YouTubeService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.apiKey = getApiKeyFromEnv();
    }

    private String getApiKeyFromEnv() {
        String key = System.getenv("YOUTUBE_API_KEY");
        if (key == null || key.isBlank()) {
            key = System.getProperty("YOUTUBE_API_KEY");
        }
        return (key != null && !key.isBlank()) ? key.trim() : null;
    }

    public List<YouTubeItemDTO> buscar(String query, String tipo) {
        return buscar(query, tipo, false);
    }

    public List<YouTubeItemDTO> buscar(String query, String tipo, boolean realVideoOnly) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        String rawQuery = query.trim();
        String tipoUpper = (tipo != null) ? tipo.toUpperCase() : "MUSICA";
        String searchType = switch (tipoUpper) {
            case "ALBUM" -> "playlist";
            case "ARTISTA" -> "channel";
            default -> "video";
        };

        // 1. Tentar API oficial do YouTube se chave estiver presente
        if (apiKey != null) {
            try {
                List<YouTubeItemDTO> apiResults = buscarViaApiOficial(rawQuery, searchType);
                if (!apiResults.isEmpty()) {
                    return apiResults;
                }
            } catch (Exception e) {
                LOGGER.warn("Falha na API oficial do YouTube. Erro: {}", e.getMessage());
            }
        }

        // 2. Se for URL direta do YouTube
        if (rawQuery.contains("youtube.com") || rawQuery.contains("youtu.be")) {
            List<YouTubeItemDTO> urlResults = extrairItemDeUrlDireta(rawQuery, searchType);
            if (!urlResults.isEmpty()) {
                return urlResults;
            }
        }

        // Se o player precisa do vídeo real para tocar, ignoramos o iTunes (que não tem IDs de vídeo válidos)
        if (realVideoOnly) {
            return buscarViaPipedApi(rawQuery, tipoUpper);
        }

        // 3. Fallback Multi-Provedor (iTunes Music API para velocidade + metadados de alta qualidade)
        List<YouTubeItemDTO> resultados = buscarViaItunesApi(rawQuery, tipoUpper);
        if (!resultados.isEmpty()) {
            return resultados;
        }

        // 4. Fallback Piped / Invidious público
        return buscarViaPipedApi(rawQuery, tipoUpper);
    }

    private List<YouTubeItemDTO> buscarViaApiOficial(String query, String searchType) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String url = YOUTUBE_SEARCH_URL + "?part=snippet&maxResults=8&q=" + encodedQuery
                     + "&type=" + searchType + "&key=" + apiKey;

        if ("video".equals(searchType)) {
            url += "&videoCategoryId=10";
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new IOException("HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        JsonNode items = root.path("items");

        List<YouTubeItemDTO> resultados = new ArrayList<>();
        List<String> videoIds = new ArrayList<>();

        for (JsonNode item : items) {
            JsonNode idNode = item.path("id");
            JsonNode snippet = item.path("snippet");

            String title = cleanHtmlEntities(snippet.path("title").asText(""));
            String channelTitle = cleanHtmlEntities(snippet.path("channelTitle").asText(""));
            String description = snippet.path("description").asText("");
            String capaUrl = snippet.path("thumbnails").path("high").path("url").asText(
                    snippet.path("thumbnails").path("default").path("url").asText("")
            );
            String publishDate = snippet.path("publishedAt").asText("");
            Integer anoLancamento = parseAno(publishDate);

            if ("video".equals(searchType)) {
                String videoId = idNode.path("videoId").asText();
                if (!videoId.isBlank()) {
                    videoIds.add(videoId);
                    resultados.add(new YouTubeItemDTO(
                            videoId,
                            "MUSICA",
                            extrairTituloMusica(title),
                            extrairArtista(title, channelTitle),
                            null,
                            anoLancamento,
                            capaUrl,
                            "https://www.youtube.com/watch?v=" + videoId,
                            description
                    ));
                }
            } else if ("playlist".equals(searchType)) {
                String playlistId = idNode.path("playlistId").asText();
                if (!playlistId.isBlank()) {
                    resultados.add(new YouTubeItemDTO(
                            playlistId,
                            "ALBUM",
                            extrairTituloAlbum(title),
                            channelTitle,
                            null,
                            anoLancamento,
                            capaUrl,
                            "https://www.youtube.com/playlist?list=" + playlistId,
                            description
                    ));
                }
            } else if ("channel".equals(searchType)) {
                String channelId = idNode.path("channelId").asText();
                if (!channelId.isBlank()) {
                    resultados.add(new YouTubeItemDTO(
                            channelId,
                            "ARTISTA",
                            channelTitle,
                            channelTitle,
                            null,
                            anoLancamento,
                            capaUrl,
                            "https://www.youtube.com/channel/" + channelId,
                            description
                    ));
                }
            }
        }

        if (!videoIds.isEmpty()) {
            preencherDuracoes(videoIds, resultados);
        }

        return resultados;
    }

    private void preencherDuracoes(List<String> videoIds, List<YouTubeItemDTO> resultados) {
        try {
            String idsParam = String.join(",", videoIds);
            String url = YOUTUBE_VIDEOS_URL + "?part=contentDetails&id=" + idsParam + "&key=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode items = root.path("items");
                for (JsonNode item : items) {
                    String id = item.path("id").asText();
                    String isoDuration = item.path("contentDetails").path("duration").asText();
                    Integer segundos = parseIsoDuration(isoDuration);

                    for (int i = 0; i < resultados.size(); i++) {
                        YouTubeItemDTO current = resultados.get(i);
                        if (current.youtubeId().equals(id)) {
                            resultados.set(i, new YouTubeItemDTO(
                                    current.youtubeId(),
                                    current.tipo(),
                                    current.titulo(),
                                    current.artistaOuCanal(),
                                    segundos,
                                    current.anoLancamento(),
                                    current.capaUrl(),
                                    current.youtubeUrl(),
                                    current.descricao()
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Erro ao buscar durações do YouTube: {}", e.getMessage());
        }
    }

    private List<YouTubeItemDTO> buscarViaItunesApi(String query, String tipo) {
        List<YouTubeItemDTO> lista = new ArrayList<>();
        try {
            String entity = switch (tipo) {
                case "ALBUM" -> "album";
                case "ARTISTA" -> "musicArtist";
                default -> "song";
            };

            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://itunes.apple.com/search?term=" + encodedQuery + "&entity=" + entity + "&limit=8&country=BR";

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Accept", "application/json")
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode results = root.path("results");

                for (JsonNode item : results) {
                    if ("ARTISTA".equals(tipo)) {
                        String artistName = item.path("artistName").asText("");
                        String genre = item.path("primaryGenreName").asText("Música");
                        String artistId = item.path("artistId").asText("artist_" + Math.abs(artistName.hashCode()));
                        
                        if (!artistName.isBlank()) {
                            String ytUrl = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(artistName, StandardCharsets.UTF_8);
                            lista.add(new YouTubeItemDTO(
                                    "yt_channel_" + Math.abs(artistName.hashCode()),
                                    "ARTISTA",
                                    artistName,
                                    artistName,
                                    null,
                                    java.time.Year.now().getValue(),
                                    "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=300&auto=format&fit=crop", // imagem artística elegante
                                    ytUrl,
                                    "Gênero: " + genre
                            ));
                        }
                    } else if ("ALBUM".equals(tipo)) {
                        String collectionName = item.path("collectionName").asText("");
                        String artistName = item.path("artistName").asText("");
                        String rawCover = item.path("artworkUrl100").asText("");
                        String hdCover = rawCover.replace("100x100bb", "600x600bb");
                        String releaseDate = item.path("releaseDate").asText("");
                        Integer ano = parseAno(releaseDate);
                        String collectionId = item.path("collectionId").asText("album_" + Math.abs(collectionName.hashCode()));

                        if (!collectionName.isBlank()) {
                            String ytUrl = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(artistName + " " + collectionName + " album", StandardCharsets.UTF_8);
                            lista.add(new YouTubeItemDTO(
                                    "yt_playlist_" + collectionId,
                                    "ALBUM",
                                    collectionName,
                                    artistName,
                                    null,
                                    ano,
                                    hdCover.isEmpty() ? "/public/img/default-cover.png" : hdCover,
                                    ytUrl,
                                    "Álbum de " + artistName
                            ));
                        }
                    } else { // MUSICA
                        String trackName = item.path("trackName").asText("");
                        String artistName = item.path("artistName").asText("");
                        String rawCover = item.path("artworkUrl100").asText("");
                        String hdCover = rawCover.replace("100x100bb", "600x600bb");
                        String releaseDate = item.path("releaseDate").asText("");
                        Integer ano = parseAno(releaseDate);
                        int trackTimeMillis = item.path("trackTimeMillis").asInt(0);
                        Integer segundos = trackTimeMillis > 0 ? trackTimeMillis / 1000 : null;
                        String trackId = item.path("trackId").asText("track_" + Math.abs(trackName.hashCode()));

                        if (!trackName.isBlank()) {
                            String ytSearchUrl = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(artistName + " - " + trackName, StandardCharsets.UTF_8);
                            lista.add(new YouTubeItemDTO(
                                    "yt_video_" + trackId,
                                    "MUSICA",
                                    trackName,
                                    artistName,
                                    segundos,
                                    ano,
                                    hdCover.isEmpty() ? "/public/img/default-cover.png" : hdCover,
                                    ytSearchUrl,
                                    "Faixa de " + artistName
                            ));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Erro na busca pública do iTunes API: {}", e.getMessage());
        }
        return lista;
    }

    private List<YouTubeItemDTO> buscarViaPipedApi(String query, String tipo) {
        List<YouTubeItemDTO> lista = new ArrayList<>();
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String url = "https://pipedapi.kavin.rocks/search?q=" + encodedQuery + "&filter=all";

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(4))
                    .GET()
                    .build();

            HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            if (res.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(res.body());
                JsonNode items = root.path("items");

                for (JsonNode item : items) {
                    if (lista.size() >= 6) break;
                    String itemUrl = item.path("url").asText("");
                    String title = item.path("title").asText("");
                    String uploaderName = item.path("uploaderName").asText("");
                    String thumbnail = item.path("thumbnail").asText("");
                    int duration = item.path("duration").asInt(0);

                    if (itemUrl.contains("/watch?v=")) {
                        String videoId = itemUrl.substring(itemUrl.indexOf("v=") + 2);
                        lista.add(new YouTubeItemDTO(
                                videoId,
                                "MUSICA",
                                extrairTituloMusica(title),
                                extrairArtista(title, uploaderName),
                                duration > 0 ? duration : null,
                                java.time.Year.now().getValue(),
                                thumbnail.isEmpty() ? "https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg" : thumbnail,
                                "https://www.youtube.com/watch?v=" + videoId,
                                "Resultado de busca do YouTube"
                        ));
                    }
                }
            }
        } catch (Exception ignored) {}
        return lista;
    }

    private List<YouTubeItemDTO> extrairItemDeUrlDireta(String url, String searchType) {
        try {
            String videoId = null;
            if (url.contains("v=")) {
                int start = url.indexOf("v=") + 2;
                int end = url.indexOf('&', start);
                videoId = end > 0 ? url.substring(start, end) : url.substring(start);
            } else if (url.contains("youtu.be/")) {
                int start = url.indexOf("youtu.be/") + 9;
                int end = url.indexOf('?', start);
                videoId = end > 0 ? url.substring(start, end) : url.substring(start);
            }

            if (videoId != null && !videoId.isBlank()) {
                String oEmbedUrl = "https://www.youtube.com/oembed?url=" + URLEncoder.encode(url, StandardCharsets.UTF_8) + "&format=json";
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(oEmbedUrl)).timeout(Duration.ofSeconds(4)).GET().build();
                HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    JsonNode json = objectMapper.readTree(res.body());
                    String title = json.path("title").asText("");
                    String author = json.path("author_name").asText("");
                    String thumb = json.path("thumbnail_url").asText("https://img.youtube.com/vi/" + videoId + "/hqdefault.jpg");

                    return List.of(new YouTubeItemDTO(
                            videoId,
                            "MUSICA",
                            extrairTituloMusica(title),
                            extrairArtista(title, author),
                            null,
                            java.time.Year.now().getValue(),
                            thumb,
                            "https://www.youtube.com/watch?v=" + videoId,
                            "Vídeo importado via URL"
                    ));
                }
            }
        } catch (Exception e) {
            LOGGER.warn("Erro ao extrair URL do YouTube: {}", e.getMessage());
        }
        return List.of();
    }

    private String extrairTituloMusica(String rawTitle) {
        if (rawTitle.contains(" - ")) {
            String[] parts = rawTitle.split(" - ", 2);
            return parts[1].replaceAll("(?i)\\([^)]*video[^)]*\\)", "")
                           .replaceAll("(?i)\\[[^\\]]*video[^\\]]*\\]", "")
                           .replaceAll("(?i)\\(official video\\)", "")
                           .replaceAll("(?i)\\(lyric video\\)", "")
                           .trim();
        }
        return rawTitle;
    }

    private String extrairArtista(String rawTitle, String channelTitle) {
        if (rawTitle.contains(" - ")) {
            String[] parts = rawTitle.split(" - ", 2);
            return parts[0].trim();
        }
        return channelTitle.replaceAll("(?i)- Topic$", "").replaceAll("(?i)VEVO$", "").trim();
    }

    private String extrairTituloAlbum(String rawTitle) {
        return rawTitle.replaceAll("(?i)\\b(album|full album|playlist)\\b", "").trim();
    }

    private Integer parseAno(String isoDate) {
        if (isoDate != null && isoDate.length() >= 4) {
            try {
                return Integer.parseInt(isoDate.substring(0, 4));
            } catch (NumberFormatException ignored) {}
        }
        return java.time.Year.now().getValue();
    }

    private Integer parseIsoDuration(String isoDuration) {
        if (isoDuration == null || isoDuration.isBlank()) return null;
        try {
            Duration duration = Duration.parse(isoDuration);
            return (int) duration.getSeconds();
        } catch (Exception e) {
            return null;
        }
    }

    private String cleanHtmlEntities(String input) {
        if (input == null) return "";
        return input.replace("&quot;", "\"")
                    .replace("&amp;", "&")
                    .replace("&#39;", "'")
                    .replace("&lt;", "<")
                    .replace("&gt;", ">");
    }
}
