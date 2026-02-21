package dev.aa.labeling.interfaces;

import dev.aa.labeling.extractors.TopicsListExtractor;

public interface IfDownloader {
    
    void bindAdapter(TopicsListExtractor topicsListExtractor);
    
    void bindExtractor(IfTopicLabeler extractor);
    
    void download();
}
