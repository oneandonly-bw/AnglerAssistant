package dev.aa.labeling.factory;

import dev.aa.labeling.config.Configuration;
import dev.aa.labeling.engine.BaseDownloader;
import dev.aa.labeling.extractors.TopicsListExtractor;
import dev.aa.labeling.interfaces.IfDownloader;
import dev.aa.labeling.interfaces.IfTopicLabeler;

public class DownloaderFactory {
    
    public static IfDownloader create(Configuration config, IfTopicLabeler labeler) {
        TopicsListExtractor topicsListExtractor = new TopicsListExtractor();
        
        BaseDownloader downloader = new BaseDownloader();
        downloader.bindAdapter(topicsListExtractor);
        downloader.bindExtractor(labeler);
        downloader.setConfiguration(config);
        
        return downloader;
    }
}
