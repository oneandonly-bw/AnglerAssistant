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
