package dev.aa.labeling.labeler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LabelingMetadataTest {

    @Test
    void testConstructor() {
        LabelingMetadata metadata = new LabelingMetadata("ru", "2026-01-22", 100, 50, 10);
        
        assertEquals("ru", metadata.getLanguage());
        assertEquals("2026-01-22", metadata.getExtractionDate());
        assertEquals(100, metadata.getTotalSentences());
        assertEquals(50, metadata.getTotalLabelsLoaded());
        assertEquals(10, metadata.getTotalTopicsProcessed());
    }

    @Test
    void testGetters() {
        LabelingMetadata metadata = new LabelingMetadata("en", "2026-02-14", 200, 100, 25);
        
        assertEquals("en", metadata.getLanguage());
        assertEquals("2026-02-14", metadata.getExtractionDate());
        assertEquals(200, metadata.getTotalSentences());
        assertEquals(100, metadata.getTotalLabelsLoaded());
        assertEquals(25, metadata.getTotalTopicsProcessed());
    }

    @Test
    void testZeroValues() {
        LabelingMetadata metadata = new LabelingMetadata("he", "2026-01-01", 0, 0, 0);
        
        assertEquals(0, metadata.getTotalSentences());
        assertEquals(0, metadata.getTotalLabelsLoaded());
        assertEquals(0, metadata.getTotalTopicsProcessed());
    }
}
