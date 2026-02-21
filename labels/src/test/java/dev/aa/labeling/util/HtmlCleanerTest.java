package dev.aa.labeling.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class HtmlCleanerTest {
    
    @Test
    @DisplayName("Should handle null/empty input")
    void testHandleNullEmpty() {
        assertEquals("", HtmlCleaner.cleanHtml(null));
        assertEquals("", HtmlCleaner.cleanHtml(""));
        assertEquals("", HtmlCleaner.cleanHtml("   "));
    }
    
    @Test
    @DisplayName("Should extract post content from phpBB")
    void testExtractPhpBBPostContent() {
        String html = """
            <html><body>
            <div class="navbar">Navigation</div>
            <div class="postbody">
                <div class="content">Post content here with enough text to pass the 50 character minimum filter in the cleaner</div>
            </div>
            <footer>Footer</footer>
            </body></html>
            """;
        
        String result = HtmlCleaner.cleanHtml(html);
        assertTrue(result.contains("Post content"), "Should contain 'Post content', got: " + result);
    }
    
    @Test
    @DisplayName("Should remove BBCode tags")
    void testRemoveBBCode() {
        String html = """
            <html><body>
            <div class="postbody">
                <div class="content">Text [b]bold[/b] and [url]link[/url] and [img]image[/img]</div>
            </div>
            </body></html>
            """;
        
        String result = HtmlCleaner.cleanHtml(html);
        assertFalse(result.contains("[b]"));
        assertFalse(result.contains("[/b]"));
        assertFalse(result.contains("[url]"));
    }
    
    @Test
    @DisplayName("Should remove arrow symbols")
    void testRemoveArrowSymbols() {
        String html = """
            <html><body>
            <div class="postbody">
                <div class="content">Text with arrows ⇧ and ⇩ and ↧</div>
            </div>
            </body></html>
            """;
        
        String result = HtmlCleaner.cleanHtml(html);
        assertFalse(result.contains("⇧"));
        assertFalse(result.contains("⇩"));
    }
    
    @Test
    @DisplayName("Should skip short content")
    void testSkipShortContent() {
        String html = """
            <html><body>
            <div class="postbody">
                <div class="content">Short</div>
            </div>
            </body></html>
            """;
        
        String result = HtmlCleaner.cleanHtml(html);
        assertFalse(result.contains("Short"));
    }
    
    @Test
    @DisplayName("Should add topic URL when provided")
    void testAddTopicUrl() {
        String html = """
            <html><body>
            <div class="postbody">
                <div class="content">Test content</div>
            </div>
            </body></html>
            """;
        
        String result = HtmlCleaner.cleanHtml(html, "https://forum.example.com/t=123");
        assertTrue(result.contains("Topic URL:"));
        assertTrue(result.contains("t=123"));
    }
    
    @Test
    @DisplayName("Should remove signatures")
    void testRemoveSignatures() {
        String html = """
            <html><body>
            <div class="postbody">
                <div class="content">Main content</div>
                <div class="signature">User signature</div>
            </div>
            </body></html>
            """;
        
        String result = HtmlCleaner.cleanHtml(html);
        assertFalse(result.contains("signature"));
    }
    
    @Test
    @DisplayName("Should remove image galleries")
    void testRemoveImageGalleries() {
        String html = """
            <html><body>
            <div class="postbody">
                <div class="content">Post content</div>
                <a href="PhotoAlbums">Gallery</a>
                <img src="thumb.jpg">
            </div>
            </body></html>
            """;
        
        String result = HtmlCleaner.cleanHtml(html);
        assertFalse(result.contains("PhotoAlbums"));
        assertFalse(result.contains("thumb.jpg"));
    }
    
    @Test
    @DisplayName("Should remove navigation links")
    void testRemoveNavigationLinks() {
        String html = """
            <html><body>
            <div class="postbody">
                <div class="content">Post content</div>
                <a href="viewforum.php">Forum</a>
                <a href="index.php">Home</a>
            </div>
            </body></html>
            """;
        
        String result = HtmlCleaner.cleanHtml(html);
        assertFalse(result.contains("viewforum.php"));
        assertFalse(result.contains("index.php"));
    }
    
    @Test
    @DisplayName("Should skip likely navigation content")
    void testSkipNavigationContent() {
        String html = """
            <html><body>
            <div class="postbody">
                <div class="content">Long navigation ↳ text that looks like menu structure with many items about forum sections and categories 2020 2021 2022 2023 2024 2025 2026</div>
            </div>
            </body></html>
            """;
        
        String result = HtmlCleaner.cleanHtml(html);
        assertFalse(result.contains("Long navigation"));
    }
    
    @Test
    @DisplayName("Should return empty when no post content found")
    void testNoPostContent() {
        String html = """
            <html><body>
            <div class="navbar">Nav</div>
            <header>Header</header>
            </body></html>
            """;
        
        String result = HtmlCleaner.cleanHtml(html);
        assertEquals("", result);
    }
}
