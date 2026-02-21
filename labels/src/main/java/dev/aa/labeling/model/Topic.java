package dev.aa.labeling.model;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

public class Topic {
    
    public enum ProcessingStatus {
        PENDING,
        DOWNLOADED,
        CLEANED,
        PROCESSED,
        ERROR
    }
    
    public enum ErrorFlag {
        DOWNLOAD_FAILED,
        CLEANING_FAILED,
        MEMORY_THRESHOLD_EXCEEDED,
        EXTRACTION_FAILED
    }
    
    private final String sourceName;
    private final String siteName;
    private final String forumName;
    private final String forumUrl;
    private final String topicUrl;
    private final String topicId;
    
    private String content;
    private String cleanedContent;
    private String author;
    private LocalDateTime creationDate;
    private String language;
    
    private ProcessingStatus processingStatus;
    private final Set<ErrorFlag> errorFlags;
    
    public Topic(String sourceName, String siteName, String forumName, String forumUrl, String topicUrl, String topicId) {
        this.sourceName = sourceName;
        this.siteName = siteName;
        this.forumName = forumName;
        this.forumUrl = forumUrl;
        this.topicUrl = topicUrl;
        this.topicId = topicId;
        this.processingStatus = ProcessingStatus.PENDING;
        this.errorFlags = new HashSet<>();
    }
    
    public String getSourceName() { return sourceName; }
    public String getSiteName() { return siteName; }
    public String getForumName() { return forumName; }
    public String getForumUrl() { return forumUrl; }
    public String getTopicUrl() { return topicUrl; }
    public String getTopicId() { return topicId; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getCleanedContent() { return cleanedContent; }
    public void setCleanedContent(String cleanedContent) { this.cleanedContent = cleanedContent; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }
    
    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }
    
    public ProcessingStatus getProcessingStatus() { return processingStatus; }
    public void setProcessingStatus(ProcessingStatus processingStatus) { this.processingStatus = processingStatus; }
    
    public Set<ErrorFlag> getErrorFlags() { return errorFlags; }
    public void addErrorFlag(ErrorFlag flag) { this.errorFlags.add(flag); }
    public boolean hasErrorFlag(ErrorFlag flag) { return this.errorFlags.contains(flag); }
}
