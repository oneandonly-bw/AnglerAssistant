package dev.aa.labeling.config;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigurationTest {
    
    @Test
    void testSiteConfiguration() {
        SiteConfiguration site = new SiteConfiguration("israfish", "IsraFish", "https://forum.israfish.co.il/", 30000, "Agent");
        
        assertEquals("israfish", site.siteId());
        assertEquals("IsraFish", site.name());
        assertEquals("https://forum.israfish.co.il/", site.baseUrl());
        assertEquals(30000, site.httpTimeout());
        assertEquals("Agent", site.httpUserAgent());
    }
    
    @Test
    void testRuntimeConfiguration() {
        RuntimeConfiguration runtime = new RuntimeConfiguration(0.8, 3);
        
        assertEquals(0.8, runtime.memoryThreshold());
        assertEquals(3, runtime.maxRetries());
    }
    
    @Test
    void testForumConfiguration() {
        ForumConfiguration forum = new ForumConfiguration(
            "http://example.com", 
            "Test", 
            "fresh", 
            true,
            "PHPBB",
            "RU",
            null,
            null
        );
        
        assertEquals("http://example.com", forum.url());
        assertEquals("Test", forum.forumName());
        assertEquals("fresh", forum.path());
        assertTrue(forum.enabled());
        assertEquals("PHPBB", forum.forumType());
        assertEquals("RU", forum.language());
    }
    
    @Test
    void testFilterConfig() {
        FilterConfig filter = new FilterConfig("Объявления", "SECTION");
        
        assertEquals("Объявления", filter.name());
        assertEquals("SECTION", filter.type());
    }
    
    @Test
    void testMetaConfiguration() {
        MetaConfiguration meta = new MetaConfiguration(
            java.util.List.of("PHPBB", "VBULLETIN"),
            java.util.List.of("SECTION", "TOPIC"),
            java.util.List.of("RU", "EN", "HE")
        );
        
        assertEquals(2, meta.forumTypes().size());
        assertTrue(meta.forumTypes().contains("PHPBB"));
        assertEquals(2, meta.filterTypes().size());
        assertTrue(meta.filterTypes().contains("SECTION"));
        assertEquals(3, meta.languages().size());
        assertTrue(meta.languages().contains("RU"));
    }
    
    @Test
    void testGeneralConfiguration() {
        GeneralConfiguration general = new GeneralConfiguration("INFO");
        
        assertEquals("INFO", general.loggingLevel());
    }
    
    @Test
    void testConfiguration() {
        Configuration config = new Configuration(
            new MetaConfiguration(java.util.List.of("PHPBB"), java.util.List.of("SECTION"), java.util.List.of("RU", "EN", "HE")),
            new GeneralConfiguration("INFO"),
            new SiteConfiguration("israfish", "IsraFish", "https://forum.israfish.co.il/", 30000, "Agent"),
            new RuntimeConfiguration(0.8, 3),
            new LabelerConfiguration(true, 15, 200, 0.3, 0.2, java.util.List.of(), 1000, java.nio.file.Path.of("data"), java.nio.file.Path.of("output/labels"), "labels.json", null, null, 0),
            java.util.List.of()
        );
        
        assertEquals("INFO", config.general().loggingLevel());
        assertEquals("israfish", config.site().siteId());
        assertTrue(config.labeler().enabled());
        assertEquals(1, config.meta().forumTypes().size());
    }
}
