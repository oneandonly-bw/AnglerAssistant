package dev.aa.labeling.labeler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class OutputWriter implements AutoCloseable {
    private final Path outputDirectory;
    private final String outputFileName;
    private final ObjectMapper objectMapper;
    private final Set<String> labelsSeen = new HashSet<>();
    private BufferedWriter writer;
    private int sentencesWritten = 0;

    public OutputWriter(Path outputDirectory, String outputFileName) {
        this.outputDirectory = outputDirectory;
        this.outputFileName = outputFileName;
        
        this.objectMapper = JsonMapper.builder()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();
    }

    public void open() throws IOException {
        if (!Files.exists(outputDirectory)) {
            Files.createDirectories(outputDirectory);
        }
        Path outputFile = outputDirectory.resolve(outputFileName);
        
        boolean fileExists = Files.exists(outputFile);
        writer = Files.newBufferedWriter(outputFile, 
            StandardOpenOption.CREATE, 
            StandardOpenOption.APPEND);
        
        if (!fileExists || Files.size(outputFile) == 0) {
            System.out.println("Creating new output file: " + outputFile);
        } else {
            System.out.println("Appending to existing output file: " + outputFile);
        }
    }

    public void registerLabel(String label) {
        if (label != null) {
            labelsSeen.add(label);
        }
    }

    private String toJson(ObjectNode json) throws IOException {
        String compact = objectMapper.writeValueAsString(json);
        StringBuilder sb = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < compact.length(); i++) {
            char c = compact.charAt(i);
            if (c == '"') {
                inString = !inString;
                sb.append(c);
            } else if (inString) {
                sb.append(c);
            } else if (c == ':') {
                sb.append(" : ");
            } else if (c == ',') {
                sb.append(", ");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public void writeForumStart(String forumUrl) throws IOException {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "forum_start");
        json.put("forumUrl", forumUrl);
        json.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        writer.write(toJson(json));
        writer.newLine();
    }

    public void writeForumEnd(String forumUrl) throws IOException {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "forum_end");
        json.put("forumUrl", forumUrl);
        writer.write(toJson(json));
        writer.newLine();
    }

    public void writeTopicStart(String forumUrl, String topicUrl) throws IOException {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "topic_start");
        json.put("forumUrl", forumUrl);
        json.put("topicUrl", topicUrl);
        writer.write(toJson(json));
        writer.newLine();
    }

    public void writeTopicEnd(String forumUrl, String topicUrl) throws IOException {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "topic_end");
        json.put("forumUrl", forumUrl);
        json.put("topicUrl", topicUrl);
        writer.write(toJson(json));
        writer.newLine();
    }

    public void writeData(LabeledSentence sentence) throws IOException {
        sentencesWritten++;
        
        for (LabelEntry label : sentence.labels()) {
            registerLabel(label.canonical());
        }

        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "data");
        json.put("forumUrl", sentence.forumUrl());
        json.put("topicUrl", sentence.topicUrl());
        json.put("lang", sentence.lang());
        json.put("text", sentence.text());
        json.set("labels", labelsToArrayNode(sentence.labels()));

        writer.write(toJson(json));
        writer.newLine();
    }

    private ArrayNode labelsToArrayNode(List<LabelEntry> labels) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        if (labels != null) {
            for (LabelEntry label : labels) {
                ObjectNode labelObj = objectMapper.createObjectNode();
                labelObj.put("surface", label.surface());
                labelObj.put("canonical", label.canonical());
                if (label.variant() != null) {
                    labelObj.put("variant", label.variant());
                }
                labelObj.put("start", label.start());
                labelObj.put("end", label.end());
                arrayNode.add(labelObj);
            }
        }
        return arrayNode;
    }

    public void writeSummary() throws IOException {
        Path summaryFile = outputDirectory.resolve(outputFileName.replace(".json", "_summary.json"));
        
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("sentences", sentencesWritten);
        
        ArrayNode labelsArray = objectMapper.createArrayNode();
        for (String label : labelsSeen.stream().sorted().toList()) {
            labelsArray.add(label);
        }
        summary.set("labels", labelsArray);
        
        String jsonString = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary);
        Files.writeString(summaryFile, jsonString);
        
        System.out.println("Summary written to: " + summaryFile);
    }

    public void write(LabelingResult result) throws IOException {
        open();
        try {
            for (LabeledSentence sentence : result.sentences()) {
                writeData(sentence);
            }
        } finally {
            close();
        }
    }

    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        }
    }
}
