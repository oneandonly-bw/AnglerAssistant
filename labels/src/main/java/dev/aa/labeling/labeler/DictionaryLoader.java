package dev.aa.labeling.labeler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class DictionaryLoader {
    private final ObjectMapper objectMapper;

    public DictionaryLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<DictionaryEntry> loadDictionaries(List<String> paths, String language) {
        List<DictionaryEntry> entries = new ArrayList<>();

        if (paths == null || paths.isEmpty()) {
            System.err.println("No dictionary paths configured");
            return entries;
        }

        for (String path : paths) {
            try {
                List<DictionaryEntry> loaded = loadDictionary(path, language);
                entries.addAll(loaded);
                System.err.println("Loaded " + loaded.size() + " entries from " + path + " (lang: " + language + ")");
            } catch (Exception e) {
                System.err.println("Error loading dictionary " + path + ": " + e.getMessage());
            }
        }

        if (entries.isEmpty()) {
            throw new RuntimeException("No dictionaries loaded - fatal error, cannot continue");
        }

        return entries;
    }

    private List<DictionaryEntry> loadDictionary(String pathStr, String language) throws Exception {
        InputStream inputStream = null;
        
        Path path = Path.of(pathStr);
        if (Files.exists(path)) {
            inputStream = Files.newInputStream(path);
        } else {
            inputStream = getClass().getClassLoader().getResourceAsStream(pathStr);
        }
        
        if (inputStream == null) {
            throw new RuntimeException("Dictionary file not found: " + pathStr);
        }

        try (InputStream is = inputStream) {
            JsonNode root = objectMapper.readTree(is);
            JsonNode data = root.get("data");

            List<DictionaryEntry> entries = new ArrayList<>();

            if (data != null && data.isArray()) {
                for (JsonNode item : data) {
                    String uid = item.has("uid") ? item.get("uid").asText() : "";
                    String type = item.has("type") ? item.get("type").asText() : "";

                    List<DictValue> values = new ArrayList<>();
                    String[] langFields;

                    if (language != null && !language.isEmpty()) {
                        langFields = new String[]{language.toLowerCase()};
                    } else {
                        langFields = new String[]{"en", "ru", "he"};
                    }

                    for (String lang : langFields) {
                        JsonNode langNode = item.get(lang);
                        if (langNode != null && langNode.isArray()) {
                            for (JsonNode nameNode : langNode) {
                                String value = nameNode.get("value").asText();
                                String specificity = nameNode.has("specificity") 
                                    ? nameNode.get("specificity").asText() 
                                    : "VARIANT";
                                
                                Duality duality = null;
                                JsonNode dualityNode = nameNode.get("duality");
                                if (dualityNode != null) {
                                    String rule = dualityNode.has("rule") ? dualityNode.get("rule").asText() : null;
                                    String alternateMeaning = dualityNode.has("alternate_meaning") 
                                        ? dualityNode.get("alternate_meaning").asText() 
                                        : null;
                                    duality = new Duality(rule, alternateMeaning);
                                }
                                
                                values.add(new DictValue(value, specificity, duality));
                            }
                        }
                    }

                    if (!values.isEmpty()) {
                        entries.add(new DictionaryEntry(uid, type, values));
                    }
                }
            }

            return entries;
        }
    }
}
