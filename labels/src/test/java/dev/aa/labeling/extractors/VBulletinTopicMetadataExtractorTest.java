package dev.aa.labeling.extractors;

import dev.aa.labeling.model.Topic;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VBulletinTopicMetadataExtractorTest {
    
    private final VBulletinTopicMetadataExtractor extractor = new VBulletinTopicMetadataExtractor();
    
    @Test
    void testExtractMetadata() {
        String html = """
            <html>
            <body>
                <a class="username">TestUser</a>
                <span datetime="2006-11-18T13:40:00">Test</span>
            </body>
            </html>
            """;
        
        Topic topic = new Topic("test", "Test", "Forum", "http://test.com/f=1", "http://test.com/t=1", "1");
        extractor.extractMetadata(topic, html);
        
        assertEquals("TestUser", topic.getAuthor());
        assertNotNull(topic.getCreationDate());
    }
    
    @Test
    void testExtractMetadataAlternateDateFormat() {
        String html = """
            <html>
            <body>
                <a class="username">TestUser</a>
                <span>2006-11-18 13:40:00</span>
            </body>
            </html>
            """;
        
        Topic topic = new Topic("test", "Test", "Forum", "http://test.com/f=1", "http://test.com/t=1", "1");
        extractor.extractMetadata(topic, html);
        
        assertEquals("TestUser", topic.getAuthor());
        assertNotNull(topic.getCreationDate());
    }
    
    @Test
    void testExtractTitle() {
        String html = """
            <html>
            <head><title>Test Topic Title</title></head>
            </html>
            """;
        
        String title = extractor.extractTitle(html);
        
        assertEquals("Test Topic Title", title);
    }
    
    @Test
    void testExtractMetadataNullContent() {
        Topic topic = new Topic("test", "Test", "Forum", "http://test.com/f=1", "http://test.com/t=1", "1");
        extractor.extractMetadata(topic, null);
        
        assertNull(topic.getAuthor());
        assertNull(topic.getCreationDate());
    }
}
