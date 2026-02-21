package dev.aa.labeling.util;

import dev.aa.labeling.Constants;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PathsManager {
    
    private static final String CONFIG_DIR = "config";
    private static final String FORUMS_SUBDIR = "forums";
    
    public static Path getSubcategoryPath(String sourceName, String subcategoryName) {
        return java.nio.file.Paths.get(Constants.DATA_ROOT)
                .resolve(Constants.RAW_DIR)
                .resolve(sourceName)
                .resolve(subcategoryName)
                .toAbsolutePath();
    }
    
    public static Path getForumConfigDirectory() {
        return java.nio.file.Paths.get("src", "main", "resources", CONFIG_DIR, FORUMS_SUBDIR)
                .toAbsolutePath();
    }
    
    public static Path getForumConfigPath(String configFileName) {
        if (!configFileName.endsWith(".json")) {
            configFileName += ".json";
        }
        return getForumConfigDirectory().resolve(configFileName);
    }
    
    public static List<String> listForumConfigs() throws IOException {
        Path configDir = getForumConfigDirectory();
        ensureDirectoryExists(configDir);
        
        try (var stream = Files.list(configDir)) {
            return stream
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .toList();
        }
    }
    
    public static Path getSourcePath(String sourceName) {
        return java.nio.file.Paths.get(Constants.DATA_ROOT)
                .resolve(Constants.RAW_DIR)
                .resolve(sourceName)
                .toAbsolutePath();
    }
    
    public static void ensureDirectoryExists(Path path) {
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create directory: " + path, e);
            }
        }
    }
    
    public static void validatePath(Path path) {
        if (path == null) {
            throw new RuntimeException("Path cannot be null");
        }
        if (!Files.exists(path)) {
            throw new RuntimeException("Path does not exist: " + path);
        }
        Path parent = path.getParent();
        if (parent == null) {
            parent = path.toAbsolutePath().getRoot();
        }
        if (parent != null && !Files.isWritable(parent)) {
            throw new RuntimeException("Path is not writable: " + path);
        }
    }
    
    public static void ensureSourceStructure(String sourceName) {
        Path sourcePath = getSourcePath(sourceName);
        ensureDirectoryExists(sourcePath);
        validatePath(sourcePath);
    }
    
    public static void ensureSubcategoryStructure(String sourceName, String subcategoryName) {
        Path subcategoryPath = getSubcategoryPath(sourceName, subcategoryName);
        ensureDirectoryExists(subcategoryPath);
        validatePath(subcategoryPath);
    }
}
