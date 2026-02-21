package dev.aa.labeling.extractors;

import dev.aa.labeling.model.Topic;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PhpBBTopicMetadataExtractorTest {
    
    private final PhpBBTopicMetadataExtractor extractor = new PhpBBTopicMetadataExtractor();
    
    @Test
    void testExtractMetadata() {
        String html = """
            <html>
            <body>
                <div class="postbody">
                    <p class="author">
                        <strong>Stasgl</strong>
                    </p>
                    <div class="content">
                        <span>Stasgl &raquo; 18-11-2006 13:40</span>
                        Test content
                    </div>
                </div>
            </body>
            </html>
            """;
        
        Topic topic = new Topic("test", "Test", "Forum", "http://test.com/f=1", "http://test.com/t=1", "1");
        extractor.extractMetadata(topic, html);
        
        assertEquals("Stasgl", topic.getAuthor());
    }
    
    @Test
    void testExtractTitle() {
        String html = """
            <html>
            <head><title>Пятница 17-е - троллинг Ашдод - Форум 'Рыбка'</title></head>
            </html>
            """;
        
        String title = extractor.extractTitle(html);
        
        assertNotNull(title);
        assertTrue(title.contains("троллинг"));
        assertFalse(title.contains("Форум"));
    }
    
    @Test
    void testExtractTitleNoTitle() {
        String html = "<html><body>No title</body></html>";
        
        String title = extractor.extractTitle(html);
        
        assertNull(title);
    }
    
    @Test
    void testExtractMetadataNullContent() {
        Topic topic = new Topic("test", "Test", "Forum", "http://test.com/f=1", "http://test.com/t=1", "1");
        extractor.extractMetadata(topic, null);
        
        assertNull(topic.getAuthor());
        assertNull(topic.getCreationDate());
    }
}
