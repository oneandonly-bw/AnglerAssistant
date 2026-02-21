package dev.aa.labeling.engine;

import dev.aa.labeling.config.Configuration;
import dev.aa.labeling.config.ForumConfiguration;
import dev.aa.labeling.interfaces.IfDownloader;
import dev.aa.labeling.interfaces.IfTopicLabeler;
import dev.aa.labeling.extractors.TopicsListExtractor;
import dev.aa.labeling.extractors.ForumType;
import dev.aa.labeling.extractors.TopicMetadataExtractor;
import dev.aa.labeling.extractors.PhpBBTopicMetadataExtractor;
import dev.aa.labeling.extractors.VBulletinTopicMetadataExtractor;
import dev.aa.labeling.labeler.MaxSentencesReachedException;
import dev.aa.labeling.model.Topic;
import dev.aa.labeling.Constants;
import dev.aa.labeling.util.HtmlCleaner;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.List;


public class BaseDownloader implements IfDownloader {
    
    private final Runtime runtime = Runtime.getRuntime();
    private TopicsListExtractor topicsListExtractor;
    private TopicMetadataExtractor metadataExtractor;
    private IfTopicLabeler extractor;
    private Configuration config;
    private java.util.Set<String> skipTopicUrls = java.util.Collections.emptySet();
    
    @Override
    public void bindAdapter(TopicsListExtractor topicsListExtractor) {
        this.topicsListExtractor = topicsListExtractor;
    }
    
    @Override
    public void bindExtractor(IfTopicLabeler extractor) {
        this.extractor = extractor;
    }
    
    public void setConfiguration(Configuration config) {
        this.config = config;
    }
    
    public void setSkipTopicUrls(java.util.Set<String> urls) {
        this.skipTopicUrls = urls != null ? urls : java.util.Collections.emptySet();
    }
    
    @Override
    public void download() {
        if (config == null || extractor == null) {
            throw new IllegalStateException("Configuration and TopicExtractor must be set");
        }
        
        if (topicsListExtractor == null) {
            topicsListExtractor = new TopicsListExtractor(config.site().baseUrl());
        }
        
        List<ForumConfiguration> forums = config.forums();
        
        if (forums.isEmpty()) {
            System.out.println("No forums to process");
            return;
        }
        
        for (ForumConfiguration forum : forums) {
            if (forum == null) {
                continue;
            }
            
            if (!forum.enabled()) {
                continue;
            }
            
            processForum(forum);
        }
    }
    
    private void processForum(ForumConfiguration forum) {
        System.out.println("Processing forum: " + forum.forumName());
        
        this.metadataExtractor = createMetadataExtractor(forum.forumType());
        
        List<String> topicUrls = topicsListExtractor.getTopicsList(
            ForumType.fromValue(forum.forumType()), 
            forum.url()
        );
        System.out.println("Found " + topicUrls.size() + " topics");
        
        try {
            for (String topicUrl : topicUrls) {
                if (!skipTopicUrls.isEmpty() && skipTopicUrls.contains(topicUrl)) {
                    System.out.println("Skipping already completed topic: " + topicUrl);
                    continue;
                }
                processTopicWithRetry(topicUrl, forum);
                if (extractor.isStopped()) {
                    System.out.println("Labeler stopped, exiting topic loop");
                    break;
                }
                try {
                    Thread.sleep(Constants.DEFAULT_REQUEST_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (MaxSentencesReachedException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void processTopicWithRetry(String topicUrl, ForumConfiguration forum) {
        int maxRetries = config.runtime().maxRetries();
        int attempt = 0;
        
        while (attempt <= maxRetries) {
            try {
                processTopic(topicUrl, forum);
                return;
            } catch (MaxSentencesReachedException e) {
                System.out.println("MaxSentencesReachedException caught, rethrowing");
                throw e;
            } catch (Exception e) {
                if (e instanceof MaxSentencesReachedException) {
                    System.out.println("MaxSentencesReachedException instanceof caught, rethrowing");
                    throw (MaxSentencesReachedException) e;
                }
                String errorMsg = e.getMessage();
                boolean isRetryable = isRetryableError(errorMsg);
                
                if (!isRetryable) {
                    System.err.println("Skipping topic due to non-retryable error: " + topicUrl + " - " + errorMsg);
                    return;
                }
                
                attempt++;
                if (attempt > maxRetries) {
                    System.err.println("Failed to process topic after " + maxRetries + " retries: " + topicUrl + " - " + errorMsg);
                } else {
                    System.err.println("Retry " + attempt + "/" + maxRetries + " for topic: " + topicUrl);
                    try {
                        Thread.sleep(1000L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }
    
    private boolean isRetryableError(String errorMsg) {
        if (errorMsg == null) return false;
        String lower = errorMsg.toLowerCase();
        return lower.contains("status=429") || 
               lower.contains("too many requests") ||
               lower.contains("status=503") ||
               lower.contains("service unavailable") ||
               lower.contains("connection timeout") ||
               lower.contains("connect timeout") ||
               lower.contains("read timeout") ||
               lower.contains("temporary failure");
    }
    
    private void processTopic(String topicUrl, ForumConfiguration forum) {
        System.out.println("Processing topic: " + topicUrl);
        
        Topic topic = new Topic(
            config.site().siteId(),
            config.site().name(),
            forum.forumName(),
            forum.url(),
            topicUrl,
            extractTopicId(topicUrl)
        );
        topic.setLanguage(forum.language());
        
        try {
            String rawContent = downloadContent(topicUrl);
            
            if (!checkMemoryThreshold()) {
                topic.addErrorFlag(Topic.ErrorFlag.MEMORY_THRESHOLD_EXCEEDED);
                topic.setProcessingStatus(Topic.ProcessingStatus.ERROR);
                System.out.println("Skipping topic - insufficient memory after download: " + topicUrl);
                return;
            }
            
            topic.setContent(rawContent);
            topic.setProcessingStatus(Topic.ProcessingStatus.DOWNLOADED);
            
            extractMetadata(topic, rawContent);
            
            String cleanedContent = HtmlCleaner.cleanHtml(rawContent);
            topic.setCleanedContent(cleanedContent);
            topic.setProcessingStatus(Topic.ProcessingStatus.CLEANED);
            
            extractor.processTopic(topic);
            topic.setProcessingStatus(Topic.ProcessingStatus.PROCESSED);
            
        } catch (MaxSentencesReachedException e) {
            throw new RuntimeException(e);
        } catch (Exception e) {
            topic.addErrorFlag(Topic.ErrorFlag.DOWNLOAD_FAILED);
            topic.setProcessingStatus(Topic.ProcessingStatus.ERROR);
            String errorDetail = e.getMessage();
            if (errorDetail == null) {
                errorDetail = e.getClass().getSimpleName();
            }
            System.err.println("Error processing topic " + topicUrl + ": " + errorDetail);
        }
    }
    
    private void extractMetadata(Topic topic, String content) {
        if (metadataExtractor != null) {
            metadataExtractor.extractMetadata(topic, content);
        }
    }
    
    private TopicMetadataExtractor createMetadataExtractor(String forumType) {
        ForumType type = ForumType.fromValue(forumType);
        return switch (type) {
            case PHPBB -> new PhpBBTopicMetadataExtractor();
            case VBULLETIN -> new VBulletinTopicMetadataExtractor();
        };
    }
    
    private String downloadContent(String url) throws Exception {
        Document doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(Constants.DEFAULT_HTTP_TIMEOUT_MS)
            .followRedirects(true)
            .get();
        return doc.html();
    }
    
    private boolean checkMemoryThreshold() {
        double threshold = config.runtime().memoryThreshold();
        double usedMemory = (double) (runtime.totalMemory() - runtime.freeMemory()) / runtime.maxMemory();
        return usedMemory <= threshold;
    }
    
    private String extractTopicId(String topicUrl) {
        // Match t=123 (topic) or p=123 (post) - prefer t over p
        String[] parts = topicUrl.split("[?&]");
        for (String part : parts) {
            if (part.startsWith("t=")) {
                return part.substring(2);
            }
        }
        // If no t= found, check for p= (post ID - less ideal)
        for (String part : parts) {
            if (part.startsWith("p=")) {
                return part.substring(2);
            }
        }
        return "";
    }
}
