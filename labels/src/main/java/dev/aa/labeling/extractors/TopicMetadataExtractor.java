package dev.aa.labeling.extractors;

import dev.aa.labeling.model.Topic;

public interface TopicMetadataExtractor {
    
    void extractMetadata(Topic topic, String htmlContent);
    
    String extractTitle(String htmlContent);
}
