package dev.aa.labeling.labeler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class CountersManager {
    private final Path countersPath;
    private final ObjectMapper mapper;
    
    private final Map<String, Integer> dictionaryCounts = new LinkedHashMap<>();
    private final Map<String, Integer> surfaceCounts = new LinkedHashMap<>();
    
    public CountersManager(Path outputDirectory, String outputFileName) {
        String baseName = outputFileName.replace(".json", "");
        this.countersPath = findUniquePath(outputDirectory, baseName);
        this.mapper = new ObjectMapper();
        this.mapper.enable(SerializationFeature.INDENT_OUTPUT);
        
        loadAndMergeCounters(outputDirectory, baseName);
    }
    
    private void loadAndMergeCounters(Path outputDirectory, String baseName) {
        try {
            Path existingCounter = findLatestCounter(outputDirectory, baseName);
            if (existingCounter != null && Files.exists(existingCounter)) {
                String json = Files.readString(existingCounter);
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
                
                com.fasterxml.jackson.databind.JsonNode dictNode = root.get("dictionary");
                if (dictNode != null && dictNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode item : dictNode) {
                        String value = item.get("value").asText();
                        int count = item.get("found").asInt();
                        dictionaryCounts.put(value, count);
                    }
                }
                
                com.fasterxml.jackson.databind.JsonNode textNode = root.get("text");
                if (textNode != null && textNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode item : textNode) {
                        String value = item.get("value").asText();
                        int count = item.get("found").asInt();
                        surfaceCounts.put(value, count);
                    }
                }
                
                System.out.println("Loaded and will merge with existing counters from: " + existingCounter);
            }
        } catch (Exception e) {
            System.err.println("No existing counters to merge: " + e.getMessage());
        }
    }
    
    private void loadExistingCounters(Path outputDirectory, String baseName) {
        try {
            Path latestCounter = findLatestCounter(outputDirectory, baseName);
            if (latestCounter != null && Files.exists(latestCounter)) {
                String json = Files.readString(latestCounter);
                com.fasterxml.jackson.databind.JsonNode root = mapper.readTree(json);
                
                com.fasterxml.jackson.databind.JsonNode dictNode = root.get("dictionary");
                if (dictNode != null && dictNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode item : dictNode) {
                        String value = item.get("value").asText();
                        int count = item.get("found").asInt();
                        dictionaryCounts.put(value, count);
                    }
                }
                
                com.fasterxml.jackson.databind.JsonNode textNode = root.get("text");
                if (textNode != null && textNode.isArray()) {
                    for (com.fasterxml.jackson.databind.JsonNode item : textNode) {
                        String value = item.get("value").asText();
                        int count = item.get("found").asInt();
                        surfaceCounts.put(value, count);
                    }
                }
                
                System.out.println("Loaded existing counters from: " + latestCounter);
            }
        } catch (Exception e) {
            System.err.println("Failed to load existing counters: " + e.getMessage());
        }
    }
    
    private Path findLatestCounter(Path outputDirectory, String baseName) {
        try {
            var files = Files.list(outputDirectory)
                .filter(p -> p.toString().contains(baseName + "_counters"))
                .filter(p -> p.toString().endsWith(".json"))
                .toList();
            
            if (files.isEmpty()) return null;
            
            return files.stream()
                .max(Comparator.comparingLong(p -> {
                    try { return Files.getLastModifiedTime(p).toMillis(); }
                    catch (Exception e) { return 0L; }
                }))
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    private Path findUniquePath(Path outputDirectory, String baseName) {
        LocalDateTime now = LocalDateTime.now();
        
        for (int hourOffset = 0; hourOffset < 24; hourOffset++) {
            for (int minOffset = 0; minOffset < 60; minOffset++) {
                for (int sec = 0; sec < 60; sec++) {
                    LocalDateTime dt = now.plusHours(hourOffset).plusMinutes(minOffset).withSecond(sec);
                    String timestamp = dt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
                    String fileName = baseName + "_" + timestamp + ".json";
                    Path path = outputDirectory.resolve(fileName);
                    if (!Files.exists(path)) {
                        return path;
                    }
                }
            }
        }
        
        String fallback = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + "_" + System.currentTimeMillis();
        return outputDirectory.resolve(baseName + "_" + fallback + ".json");
    }
    
    public void incrementDictionary(String value) {
        dictionaryCounts.merge(value, 1, Integer::sum);
    }
    
    public void incrementSurface(String value) {
        surfaceCounts.merge(value, 1, Integer::sum);
    }
    
    public void save() {
        try {
            List<Map<String, Object>> dictList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : dictionaryCounts.entrySet()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("value", entry.getKey());
                item.put("found", entry.getValue());
                dictList.add(item);
            }
            
            List<Map<String, Object>> textList = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : surfaceCounts.entrySet()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("value", entry.getKey());
                item.put("found", entry.getValue());
                textList.add(item);
            }
            
            Map<String, Object> root = new LinkedHashMap<>();
            root.put("dictionary", dictList);
            root.put("text", textList);
            
            Files.createDirectories(countersPath.getParent());
            Files.writeString(countersPath, mapper.writeValueAsString(root));
        } catch (IOException e) {
            System.err.println("Failed to save counters: " + e.getMessage());
        }
    }
    
    public int getDictionaryTotal() {
        return dictionaryCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    public int getSurfaceTotal() {
        return surfaceCounts.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    public Path getCountersPath() {
        return countersPath;
    }
}
