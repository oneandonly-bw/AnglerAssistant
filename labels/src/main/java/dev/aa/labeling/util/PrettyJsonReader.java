package dev.aa.labeling.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class PrettyJsonReader {
    private static final ObjectMapper mapper = new ObjectMapper();

    public static void readEntries(Path filePath, Consumer<JsonNode> consumer) throws IOException {
        if (!Files.exists(filePath)) {
            return;
        }

        String content = Files.readString(filePath);
        List<String> entries = parseJsonEntries(content);

        for (String entry : entries) {
            try {
                JsonNode node = mapper.readTree(entry);
                consumer.accept(node);
            } catch (Exception e) {
                System.err.println("Failed to parse entry: " + e.getMessage());
            }
        }
    }

    public static List<JsonNode> readAllEntries(Path filePath) throws IOException {
        List<JsonNode> entries = new ArrayList<>();
        readEntries(filePath, entries::add);
        return entries;
    }

    public static List<JsonNode> readDataEntries(Path filePath) throws IOException {
        List<JsonNode> dataEntries = new ArrayList<>();
        readEntries(filePath, node -> {
            if (node.has("type") && "data".equals(node.get("type").asText())) {
                dataEntries.add(node);
            }
        });
        return dataEntries;
    }

    public static void printEntry(JsonNode node) {
        try {
            String pretty = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
            System.out.println(pretty);
        } catch (Exception e) {
            System.err.println("Failed to print: " + e.getMessage());
        }
    }

    public static void printFile(Path filePath) throws IOException {
        readEntries(filePath, PrettyJsonReader::printEntry);
    }

    private static List<String> parseJsonEntries(String content) {
        List<String> entries = new ArrayList<>();
        int braceCount = 0;
        int jsonStart = -1;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);

            if (c == '{') {
                if (jsonStart == -1) {
                    jsonStart = i;
                }
                braceCount++;
            } else if (c == '}') {
                braceCount--;
                if (braceCount == 0 && jsonStart != -1) {
                    String entry = content.substring(jsonStart, i + 1);
                    entries.add(entry);
                    jsonStart = -1;
                }
            }
        }

        return entries;
    }
}
