package dev.aa.labeling.labeler;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LabelingResultTest {

    @Test
    void testConstructor() {
        List<LabeledSentence> sentences = List.of(
            new LabeledSentence("http://example.com/f1", "http://example.com/1", "ru", "text1", List.of(new LabelEntry("label1", "label1", null, 0, 6, true))),
            new LabeledSentence("http://example.com/f1", "http://example.com/2", "ru", "text2", List.of(new LabelEntry("label2", "label2", null, 0, 6, true)))
        );
        LabelingMetadata metadata = new LabelingMetadata("ru", "2026-01-01", 2, 10, 1);
        
        LabelingResult result = new LabelingResult(sentences, metadata);
        
        assertEquals(sentences, result.sentences());
        assertEquals(metadata, result.metadata());
    }

    @Test
    void testGetSentences() {
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com", "en", "test", List.of(new LabelEntry("a", "a", null, 0, 1, true)));
        LabelingResult result = new LabelingResult(List.of(sentence), new LabelingMetadata("en", "date", 1, 5, 1));
        
        assertEquals(1, result.sentences().size());
        assertEquals("test", result.sentences().get(0).text());
    }

    @Test
    void testGetMetadata() {
        LabelingMetadata metadata = new LabelingMetadata("ru", "2026-01-01", 100, 50, 10);
        LabelingResult result = new LabelingResult(List.of(), metadata);
        
        assertEquals(metadata, result.metadata());
    }

    @Test
    void testGetTotalSentences() {
        List<LabeledSentence> sentences = List.of(
            new LabeledSentence("http://example.com/f1", "http://example.com/1", "ru", "text1", List.of(new LabelEntry("l1", "l1", null, 0, 2, true))),
            new LabeledSentence("http://example.com/f1", "http://example.com/2", "ru", "text2", List.of(new LabelEntry("l2", "l2", null, 0, 2, true))),
            new LabeledSentence("http://example.com/f1", "http://example.com/3", "ru", "text3", List.of(new LabelEntry("l3", "l3", null, 0, 2, true)))
        );
        LabelingResult result = new LabelingResult(sentences, new LabelingMetadata("ru", "date", 3, 10, 1));
        
        assertEquals(3, result.getTotalSentences());
    }

    @Test
    void testEmptySentences() {
        LabelingResult result = new LabelingResult(List.of(), new LabelingMetadata("en", "date", 0, 0, 0));
        
        assertTrue(result.sentences().isEmpty());
        assertEquals(0, result.getTotalSentences());
    }
}
