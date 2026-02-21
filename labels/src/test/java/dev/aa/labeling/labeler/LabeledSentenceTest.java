package dev.aa.labeling.labeler;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LabeledSentenceTest {

    @Test
    void testConstructor() {
        List<LabelEntry> validLabels = List.of(
            new LabelEntry("carp", "carp", null, 10, 14, true),
            new LabelEntry("fish", "fish", null, 15, 19, true)
        );
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "en", "I caught a carp and fish", validLabels, List.of());
        
        assertEquals("http://example.com/topic", sentence.topicUrl());
        assertEquals("en", sentence.lang());
        assertEquals("I caught a carp and fish", sentence.text());
        assertEquals(2, sentence.validLabels().size());
    }

    @Test
    void testGetters() {
        List<LabelEntry> validLabels = List.of(new LabelEntry("карп", "карп", null, 7, 11, true));
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "ru", "я поймал карпа", validLabels, List.of());
        
        assertEquals("http://example.com/topic", sentence.topicUrl());
        assertEquals("ru", sentence.lang());
        assertEquals("я поймал карпа", sentence.text());
        assertEquals(1, sentence.validLabels().size());
    }

    @Test
    void testEmptyLabels() {
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "en", "No labels here", List.of(), List.of());
        
        assertTrue(sentence.validLabels().isEmpty());
        assertTrue(sentence.invalidLabels().isEmpty());
    }

    @Test
    void testMultipleLabels() {
        List<LabelEntry> validLabels = List.of(
            new LabelEntry("carp", "carp", null, 14, 18, true),
            new LabelEntry("pike", "pike", null, 23, 27, true)
        );
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "en", "Fishing for carp and pike", validLabels, List.of());
        
        assertEquals(2, sentence.validLabels().size());
    }

    @Test
    void testWithVariant() {
        List<LabelEntry> validLabels = List.of(
            new LabelEntry("musht", "tilapia", "musht", 10, 15, true)
        );
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "en", "I caught musht", validLabels, List.of());
        
        assertEquals("musht", sentence.validLabels().get(0).surface());
        assertEquals("tilapia", sentence.validLabels().get(0).canonical());
        assertEquals("musht", sentence.validLabels().get(0).variant());
    }
    
    @Test
    void testInvalidLabels() {
        List<LabelEntry> valid = List.of(
            new LabelEntry("carp", "carp", null, 10, 14, true)
        );
        List<LabelEntry> invalid = List.of(
            new LabelEntry("fish", "fish", null, 15, 19, false)
        );
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "en", "I caught carp and fish", valid, invalid);
        
        assertEquals(1, sentence.validLabels().size());
        assertEquals(1, sentence.invalidLabels().size());
        assertFalse(sentence.invalidLabels().get(0).isValid());
    }
}
