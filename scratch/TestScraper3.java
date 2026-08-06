import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestScraper3 {
    public static void main(String[] args) throws Exception {
        String json = Files.readString(Path.of("scratch/test_json.json"));
        Pattern pattern = Pattern.compile("\"itemSectionRenderer\":\\{\"contents\":\\[\\{(.*?)\\}\\],");
        Matcher matcher = pattern.matcher(json);
        if (matcher.find()) {
            System.out.println(matcher.group(1).substring(0, Math.min(200, matcher.group(1).length())));
        } else {
            System.out.println("No itemSectionRenderer found");
        }
    }
}
