package dev.aa.labeling.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public class CheckpointResolver {
    private static final ObjectMapper mapper = new ObjectMapper();

    public record CompletedTopics(
        Set<String> completedForumUrls,
        Set<String> completedTopicUrls
    ) {}

    public static CompletedTopics loadCompleted(Path outputFile) throws IOException {
        Set<String> completedForums = new HashSet<>();
        Set<String> completedTopics = new HashSet<>();

        if (!Files.exists(outputFile)) {
            return new CompletedTopics(completedForums, completedTopics);
        }

        try (Stream<String> lines = Files.lines(outputFile)) {
            lines.forEach(line -> {
                try {
                    JsonNode node = mapper.readTree(line);
                    String type = node.has("type") ? node.get("type").asText() : null;

                    if ("topic_end".equals(type) && node.has("topicUrl")) {
                        completedTopics.add(node.get("topicUrl").asText());
                    }
                    if ("forum_end".equals(type) && node.has("forumUrl")) {
                        completedForums.add(node.get("forumUrl").asText());
                    }
                } catch (Exception ignored) {
                }
            });
        }

        return new CompletedTopics(completedForums, completedTopics);
    }

    public static Set<String> findIncompleteTopics(Path outputFile) throws IOException {
        Set<String> startedTopics = new HashSet<>();
        Set<String> completedTopics = new HashSet<>();

        if (!Files.exists(outputFile)) {
            return Set.of();
        }

        try (Stream<String> lines = Files.lines(outputFile)) {
            lines.forEach(line -> {
                try {
                    JsonNode node = mapper.readTree(line);
                    String type = node.has("type") ? node.get("type").asText() : null;

                    if ("topic_start".equals(type) && node.has("topicUrl")) {
                        startedTopics.add(node.get("topicUrl").asText());
                    }
                    if ("topic_end".equals(type) && node.has("topicUrl")) {
                        completedTopics.add(node.get("topicUrl").asText());
                    }
                } catch (Exception ignored) {
                }
            });
        }

        Set<String> incomplete = new HashSet<>(startedTopics);
        incomplete.removeAll(completedTopics);
        return incomplete;
    }

    public static void cleanupIncomplete(Path outputFile) throws IOException {
        Set<String> incompleteTopics = findIncompleteTopics(outputFile);

        if (incompleteTopics.isEmpty()) {
            return;
        }

        Set<String> incompleteForums = new HashSet<>();

        try (Stream<String> lines = Files.lines(outputFile)) {
            lines.forEach(line -> {
                try {
                    JsonNode node = mapper.readTree(line);
                    String type = node.has("type") ? node.get("type").asText() : null;
                    if (incompleteTopics.contains(node.has("topicUrl") ? node.get("topicUrl").asText() : null)) {
                        if ("topic_start".equals(type) && node.has("forumUrl")) {
                            incompleteForums.add(node.get("forumUrl").asText());
                        }
                    }
                } catch (Exception ignored) {
                }
            });
        }

        var lines = Files.readAllLines(outputFile);
        var keptLines = new java.util.ArrayList<String>();
        String currentTopic = null;

        for (String line : lines) {
            try {
                JsonNode node = mapper.readTree(line);
                String type = node.has("type") ? node.get("type").asText() : null;
                String topicUrl = node.has("topicUrl") ? node.get("topicUrl").asText() : null;

                if ("topic_start".equals(type)) {
                    currentTopic = topicUrl;
                } else if ("topic_end".equals(type) || "forum_end".equals(type)) {
                    currentTopic = null;
                }

                if (currentTopic != null && incompleteTopics.contains(currentTopic)) {
                    continue;
                }

                keptLines.add(line);
            } catch (Exception e) {
                if (currentTopic == null || !incompleteTopics.contains(currentTopic)) {
                    keptLines.add(line);
                }
            }
        }

        Files.write(outputFile, keptLines);
    }
}
