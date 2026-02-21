package dev.aa.labeling.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TopicTest {
    
    @Test
    void testTopicCreation() {
        Topic topic = new Topic("source", "site", "forum", "http://example.com/forum1", "http://example.com/topic1", "123");
        
        assertEquals("source", topic.getSourceName());
        assertEquals("site", topic.getSiteName());
        assertEquals("forum", topic.getForumName());
        assertEquals("http://example.com/topic1", topic.getTopicUrl());
        assertEquals("123", topic.getTopicId());
        assertEquals(Topic.ProcessingStatus.PENDING, topic.getProcessingStatus());
    }
    
    @Test
    void testTopicSetters() {
        Topic topic = new Topic("source", "site", "forum", "http://example.com/forum1", "url", "1");
        
        topic.setContent("content");
        topic.setAuthor("author");
        topic.setLanguage("en");
        
        assertEquals("content", topic.getContent());
        assertEquals("author", topic.getAuthor());
        assertEquals("en", topic.getLanguage());
    }
    
    @Test
    void testErrorFlags() {
        Topic topic = new Topic("source", "site", "forum", "http://example.com/forum1", "url", "1");
        
        assertFalse(topic.hasErrorFlag(Topic.ErrorFlag.DOWNLOAD_FAILED));
        
        topic.addErrorFlag(Topic.ErrorFlag.DOWNLOAD_FAILED);
        
        assertTrue(topic.hasErrorFlag(Topic.ErrorFlag.DOWNLOAD_FAILED));
        assertTrue(topic.getErrorFlags().contains(Topic.ErrorFlag.DOWNLOAD_FAILED));
    }
}
