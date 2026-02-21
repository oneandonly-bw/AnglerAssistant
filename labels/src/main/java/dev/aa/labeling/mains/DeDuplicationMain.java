package dev.aa.labeling.mains;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import dev.aa.labeling.util.SimilarityUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DeDuplicationMain {
    
    private static final double SIMILARITY_THRESHOLD = 0.9;
    
    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.out.println("Usage: java DeDuplicationMain <inputFile> [outputFile]");
            System.out.println("  inputFile: Path to input JSONL file (e.g., output/labels/israfish_species_valid.jsonl)");
            System.out.println("  outputFile: Optional output file (default: inputFile_dedup.jsonl)");
            System.exit(1);
        }
        
        Path inputPath = Paths.get(args[0]);
        Path outputPath = args.length > 1 ? Paths.get(args[1]) : getDedupPath(inputPath);
        
        System.out.println("Input file: " + inputPath);
        System.out.println("Output file: " + outputPath);
        System.out.println("Similarity threshold: " + SIMILARITY_THRESHOLD);
        
        deduplicate(inputPath, outputPath);
    }
    
    private static Path getDedupPath(Path inputPath) {
        String fileName = inputPath.getFileName().toString();
        String baseName = fileName.replace(".jsonl", "");
        return inputPath.getParent().resolve(baseName + "_dedup.jsonl");
    }
    
    private static void deduplicate(Path inputPath, Path outputPath) throws IOException {
        ObjectMapper mapper = new JsonMapper();
        List<String> lines = Files.readAllLines(inputPath);
        
        System.out.println("Reading " + lines.size() + " lines from input file...");
        
        List<String> dataLines = new ArrayList<>();
        for (String line : lines) {
            if (line.contains("\"type\" : \"data\"")) {
                dataLines.add(line);
            }
        }
        
        System.out.println("Found " + dataLines.size() + " data records...");
        
        List<String> uniqueRecords = new ArrayList<>();
        int removedCount = 0;
        
        for (String record : dataLines) {
            String text = extractText(record, mapper);
            if (text == null || text.isEmpty()) {
                uniqueRecords.add(record);
                continue;
            }
            
            boolean isDuplicate = false;
            for (String existing : uniqueRecords) {
                String existingText = extractText(existing, mapper);
                if (existingText != null) {
                    double similarity = SimilarityUtil.calculateSimilarity(
                        text, existingText, SimilarityUtil.SimilarityType.LEVENSHTEIN);
                    
                    if (similarity >= SIMILARITY_THRESHOLD) {
                        isDuplicate = true;
                        removedCount++;
                        break;
                    }
                }
            }
            
            if (!isDuplicate) {
                uniqueRecords.add(record);
            }
        }
        
        Files.createDirectories(outputPath.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
            for (String record : uniqueRecords) {
                writer.write(record);
                writer.newLine();
            }
        }
        
        System.out.println(removedCount + " sentences removed.");
        System.out.println("Unique records written to: " + outputPath);
    }
    
    private static String extractText(String jsonLine, ObjectMapper mapper) {
        try {
            JsonNode node = mapper.readTree(jsonLine);
            JsonNode textNode = node.get("text");
            return textNode != null ? textNode.asText() : null;
        } catch (Exception e) {
            return null;
        }
    }
}
