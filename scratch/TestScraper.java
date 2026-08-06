import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TestScraper {
    public static void main(String[] args) throws Exception {
        String url = "https://www.youtube.com/results?search_query=lady+gaga+album&sp=EgIQAw%253D%253D";
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                .header("Accept-Language", "en-US,en;q=0.9")
                .timeout(Duration.ofSeconds(6))
                .GET()
                .build();
        HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
        String html = res.body();
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("var ytInitialData = (\\{.*?\\});</script>");
        java.util.regex.Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            java.nio.file.Files.writeString(java.nio.file.Path.of("scratch/test_json.json"), matcher.group(1));
            System.out.println("JSON saved to scratch/test_json.json");
        }
    }
}
