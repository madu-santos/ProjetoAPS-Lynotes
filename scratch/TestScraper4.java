import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Map;

public class TestScraper4 {
    public static void main(String[] args) throws Exception {
        String json = Files.readString(Path.of("scratch/test_json.json"));
        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(json);
        JsonNode contents = root.path("contents").path("twoColumnSearchResultsRenderer").path("primaryContents").path("sectionListRenderer").path("contents");
        JsonNode itemSection = contents.get(0).path("itemSectionRenderer").path("contents");
        if (itemSection.isArray()) {
            for (JsonNode item : itemSection) {
                Iterator<Map.Entry<String, JsonNode>> fields = item.fields();
                while (fields.hasNext()) {
                    System.out.println("Key: " + fields.next().getKey());
                }
            }
        }
    }
}
