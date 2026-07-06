import java.net.URI;
import java.net.http.*;

public class Test {
    public static void main(String[] args) throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.youtube.com/watch?v=dQw4w9WgXcQ"))
                .header("User-Agent", "Mozilla/5.0")
                .header("Accept-Language", "en-US,en;q=0.9")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("\"lengthSeconds\":\"(\\d+)\"");
        java.util.regex.Matcher m = p.matcher(body);
        if (m.find()) {
            System.out.println("FOUND: " + m.group(1));
        } else {
            System.out.println("NOT FOUND. Status: " + response.statusCode());
            System.out.println("Body length: " + body.length());
            System.out.println(body.substring(0, Math.min(body.length(), 500)));
        }
    }
}
