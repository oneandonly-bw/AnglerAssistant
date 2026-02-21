package dev.aa.labeling.labeler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CacheManager {
    private static final Logger logger = LoggerFactory.getLogger(CacheManager.class);
    
    private final Path termsSeenPath;
    private final Path lemmasSeenPath;
    
    private Set<String> termsSeen;
    private Set<String> lemmasSeen;
    
    public CacheManager(Path termsSeenPath, Path lemmasSeenPath) {
        this.termsSeenPath = termsSeenPath;
        this.lemmasSeenPath = lemmasSeenPath;
        this.termsSeen = new HashSet<>();
        this.lemmasSeen = new HashSet<>();
    }
    
    public void load() throws IOException {
        termsSeen = loadSetFile(termsSeenPath);
        lemmasSeen = loadSetFile(lemmasSeenPath);
        logger.info("Cache loaded: termsSeen={}, lemmasSeen={}", termsSeen.size(), lemmasSeen.size());
    }
    
    public void save() throws IOException {
        saveSetFile(termsSeenPath, termsSeen);
        saveSetFile(lemmasSeenPath, lemmasSeen);
        logger.info("Cache saved: termsSeen={}, lemmasSeen={}", termsSeen.size(), lemmasSeen.size());
    }
    
    private Set<String> loadSetFile(Path path) {
        Set<String> set = new HashSet<>();
        if (Files.exists(path)) {
            try {
                List<String> lines = Files.readAllLines(path);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        set.add(trimmed);
                    }
                }
                logger.debug("Loaded {} entries from {}", set.size(), path);
            } catch (IOException e) {
                logger.warn("Failed to load cache file {}: {}", path, e.getMessage());
            }
        }
        return set;
    }
    
    private void saveSetFile(Path path, Set<String> set) throws IOException {
        Path parent = path.getParent();
        if (parent != null && !Files.exists(parent)) {
            Files.createDirectories(parent);
        }
        List<String> lines = new ArrayList<>(set.stream().sorted().toList());
        Files.write(path, lines);
    }
    
    public boolean containsTerm(String term) {
        return termsSeen.contains(term);
    }
    
    public boolean containsLemma(String lemma) {
        return lemmasSeen.contains(lemma);
    }
    
    public void addTerm(String term) {
        termsSeen.add(term);
    }
    
    public void addLemma(String lemma) {
        lemmasSeen.add(lemma);
    }
    
    public int getTermsSeenCount() {
        return termsSeen.size();
    }
    
    public int getLemmasSeenCount() {
        return lemmasSeen.size();
    }
}
