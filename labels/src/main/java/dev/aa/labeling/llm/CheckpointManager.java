package dev.aa.labeling.llm;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

public class CheckpointManager {
    private static final Logger logger = LoggerFactory.getLogger(CheckpointManager.class);
    private static final String CHECKPOINT_FILENAME = "checkpoint.json";
    
    private final Path checkpointPath;
    private final Path workingDirectory;
    private final int saveIntervalTopics;
    
    private int topicsSinceLastSave = 0;

    public CheckpointManager(Path workingDirectory, int saveIntervalTopics) {
        this.workingDirectory = workingDirectory;
        this.checkpointPath = workingDirectory.resolve(CHECKPOINT_FILENAME);
        this.saveIntervalTopics = saveIntervalTopics;
    }

    public CheckpointManager(Path workingDirectory) {
        this(workingDirectory, 10);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class Checkpoint {
        private String timestamp;
        private int topicIndex;
        private int sentenceIndex;
        
        @JsonProperty("cacheState")
        private CacheState cacheState;
        
        @JsonProperty("lastProcessedUrl")
        private String lastProcessedUrl;
        
        public static class CacheState {
            private String positiveCache;
            private String negativeCache;
            
            public CacheState() {}
            
            public CacheState(String positiveCache, String negativeCache) {
                this.positiveCache = positiveCache;
                this.negativeCache = negativeCache;
            }

            public String getPositiveCache() { return positiveCache; }
            public void setPositiveCache(String positiveCache) { this.positiveCache = positiveCache; }
            public String getNegativeCache() { return negativeCache; }
            public void setNegativeCache(String negativeCache) { this.negativeCache = negativeCache; }
        }

        public Checkpoint() {
            this.timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now());
        }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
        public int getTopicIndex() { return topicIndex; }
        public void setTopicIndex(int topicIndex) { this.topicIndex = topicIndex; }
        public int getSentenceIndex() { return sentenceIndex; }
        public void setSentenceIndex(int sentenceIndex) { this.sentenceIndex = sentenceIndex; }
        public CacheState getCacheState() { return cacheState; }
        public void setCacheState(CacheState cacheState) { this.cacheState = cacheState; }
        public String getLastProcessedUrl() { return lastProcessedUrl; }
        public void setLastProcessedUrl(String lastProcessedUrl) { this.lastProcessedUrl = lastProcessedUrl; }
    }

    public void saveCheckpoint(int topicIndex, int sentenceIndex, String lastUrl) throws IOException {
        saveCheckpoint(topicIndex, sentenceIndex, lastUrl, null, null);
    }

    public void saveCheckpoint(int topicIndex, int sentenceIndex, String lastUrl, 
                               String positiveCache, String negativeCache) throws IOException {
        Checkpoint checkpoint = new Checkpoint();
        checkpoint.setTopicIndex(topicIndex);
        checkpoint.setSentenceIndex(sentenceIndex);
        checkpoint.setLastProcessedUrl(lastUrl);
        
        if (positiveCache != null || negativeCache != null) {
            checkpoint.setCacheState(new Checkpoint.CacheState(positiveCache, negativeCache));
        }

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(checkpoint);
        
        Files.writeString(checkpointPath, json);
        topicsSinceLastSave = 0;
        
        logger.info("Checkpoint saved: topic={}, sentence={}", topicIndex, sentenceIndex);
    }

    public Checkpoint loadCheckpoint() throws IOException {
        if (!hasCheckpoint()) {
            return null;
        }
        
        ObjectMapper mapper = new ObjectMapper();
        Checkpoint checkpoint = mapper.readValue(checkpointPath.toFile(), Checkpoint.class);
        
        logger.info("Checkpoint loaded: topic={}, sentence={}", 
            checkpoint.getTopicIndex(), checkpoint.getSentenceIndex());
        
        return checkpoint;
    }

    public boolean hasCheckpoint() {
        return Files.exists(checkpointPath);
    }

    public void clearCheckpoint() throws IOException {
        if (hasCheckpoint()) {
            Files.delete(checkpointPath);
            logger.info("Checkpoint cleared");
        }
    }

    public boolean shouldSave(int topicIndex) {
        topicsSinceLastSave++;
        return topicsSinceLastSave >= saveIntervalTopics;
    }

    public void saveIfNeeded(int topicIndex, int sentenceIndex, String lastUrl) throws IOException {
        if (shouldSave(topicIndex)) {
            saveCheckpoint(topicIndex, sentenceIndex, lastUrl);
        }
    }

    public Path getCheckpointPath() {
        return checkpointPath;
    }
}
