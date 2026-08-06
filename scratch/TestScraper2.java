import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestScraper2 {
    public static void main(String[] args) throws Exception {
        String json = Files.readString(Path.of("scratch/test_json.json"));
        Pattern pattern = Pattern.compile("\"playlistRenderer\":\\{.*?\"title\":(\\{.*?\\})");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            System.out.println(matcher.group(1));
        } else {
            System.out.println("No playlistRenderer found");
        }
    }
}
