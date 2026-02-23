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
    private final String outputFileNameValid;
    private final String outputFileNameInvalid;
    private final String baseFileName;
    private final ObjectMapper objectMapper;
    private final Set<String> labelsSeen = new HashSet<>();
    private BufferedWriter writerValid;
    private BufferedWriter writerInvalid;
    private int sentencesWritten = 0;
    private int writeCount = 0;

    public OutputWriter(Path outputDirectory, String outputFileName) {
        this.outputDirectory = outputDirectory;
        this.baseFileName = outputFileName.replace(".jsonl", "");
        this.outputFileNameValid = baseFileName + "_valid.jsonl";
        this.outputFileNameInvalid = baseFileName + "_invalid.jsonl";
        
        this.objectMapper = JsonMapper.builder()
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .build();
    }

    public void open() throws IOException {
        if (!Files.exists(outputDirectory)) {
            Files.createDirectories(outputDirectory);
        }
        
        Path outputFileValid = outputDirectory.resolve(outputFileNameValid);
        Path outputFileInvalid = outputDirectory.resolve(outputFileNameInvalid);
        
        boolean fileExistsValid = Files.exists(outputFileValid);
        writerValid = Files.newBufferedWriter(outputFileValid, 
            StandardOpenOption.CREATE, 
            StandardOpenOption.APPEND);
        
        boolean fileExistsInvalid = Files.exists(outputFileInvalid);
        writerInvalid = Files.newBufferedWriter(outputFileInvalid, 
            StandardOpenOption.CREATE, 
            StandardOpenOption.APPEND);
        
        if (!fileExistsValid || Files.size(outputFileValid) == 0) {
            System.out.println("Creating new output file: " + outputFileValid);
        } else {
            System.out.println("Appending to existing output file: " + outputFileValid);
        }
        
        if (!fileExistsInvalid || Files.size(outputFileInvalid) == 0) {
            System.out.println("Creating new output file: " + outputFileInvalid);
        } else {
            System.out.println("Appending to existing output file: " + outputFileInvalid);
        }
    }

    public void registerLabel(String label) {
        if (label != null) {
            labelsSeen.add(label);
        }
    }

    private String toJson(ObjectNode json) throws IOException {
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
    }

    public void writeForumStart(String forumUrl) throws IOException {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "forum_start");
        json.put("forumUrl", forumUrl);
        json.put("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
        writerValid.write(toJson(json));
        writerValid.newLine();
    }

    public void writeForumEnd(String forumUrl) throws IOException {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "forum_end");
        json.put("forumUrl", forumUrl);
        writerValid.write(toJson(json));
        writerValid.newLine();
    }

    public void writeTopicStart(String forumUrl, String topicUrl) throws IOException {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "topic_start");
        json.put("forumUrl", forumUrl);
        json.put("topicUrl", topicUrl);
        writerValid.write(toJson(json));
        writerValid.newLine();
    }

    public void writeTopicEnd(String forumUrl, String topicUrl) throws IOException {
        ObjectNode json = objectMapper.createObjectNode();
        json.put("type", "topic_end");
        json.put("forumUrl", forumUrl);
        json.put("topicUrl", topicUrl);
        writerValid.write(toJson(json));
        writerValid.newLine();
    }

    public void writeData(LabeledSentence sentence) throws IOException {
        sentencesWritten++;
        
        for (LabelEntry label : sentence.validLabels()) {
            registerLabel(label.canonical());
        }
        for (LabelEntry label : sentence.invalidLabels()) {
            registerLabel(label.canonical());
        }

        // Write valid labels
        if (!sentence.validLabels().isEmpty()) {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "data");
            json.put("forumUrl", sentence.forumUrl());
            json.put("topicUrl", sentence.topicUrl());
            json.put("lang", sentence.lang());
            json.put("text", sentence.text());
            json.set("labels", labelsToArrayNode(sentence.validLabels()));

            writerValid.write(toJson(json));
            writerValid.newLine();
            writerValid.flush();
        }
        
        // Write invalid labels
        if (!sentence.invalidLabels().isEmpty()) {
            ObjectNode json = objectMapper.createObjectNode();
            json.put("type", "data");
            json.put("forumUrl", sentence.forumUrl());
            json.put("topicUrl", sentence.topicUrl());
            json.put("lang", sentence.lang());
            json.put("text", sentence.text());
            json.set("labels", labelsToArrayNode(sentence.invalidLabels()));

            writerInvalid.write(toJson(json));
            writerInvalid.newLine();
            writerInvalid.flush();
        }
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
                labelObj.put("isValid", label.isValid());
                arrayNode.add(labelObj);
            }
        }
        return arrayNode;
    }

    public void writeSummary() throws IOException {
        Path summaryFile = outputDirectory.resolve(baseFileName + "_summary.json");
        
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
        if (writerValid != null) {
            writerValid.close();
        }
        if (writerInvalid != null) {
            writerInvalid.close();
        }
    }
}
