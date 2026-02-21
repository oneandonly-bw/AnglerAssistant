package dev.aa.labeling.labeler;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LabeledSentenceTest {

    @Test
    void testConstructor() {
        List<LabelEntry> labels = List.of(
            new LabelEntry("carp", "carp", null, 10, 14),
            new LabelEntry("fish", "fish", null, 15, 19)
        );
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "en", "I caught a carp and fish", labels);
        
        assertEquals("http://example.com/topic", sentence.topicUrl());
        assertEquals("en", sentence.lang());
        assertEquals("I caught a carp and fish", sentence.text());
        assertEquals(2, sentence.labels().size());
    }

    @Test
    void testGetters() {
        List<LabelEntry> labels = List.of(new LabelEntry("карп", "карп", null, 7, 11));
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "ru", "я поймал карпа", labels);
        
        assertEquals("http://example.com/topic", sentence.topicUrl());
        assertEquals("ru", sentence.lang());
        assertEquals("я поймал карпа", sentence.text());
        assertEquals(1, sentence.labels().size());
    }

    @Test
    void testEmptyLabels() {
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "en", "No labels here", List.of());
        
        assertTrue(sentence.labels().isEmpty());
    }

    @Test
    void testMultipleLabels() {
        List<LabelEntry> labels = List.of(
            new LabelEntry("carp", "carp", null, 14, 18),
            new LabelEntry("pike", "pike", null, 23, 27)
        );
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "en", "Fishing for carp and pike", labels);
        
        assertEquals(2, sentence.labels().size());
    }

    @Test
    void testWithVariant() {
        List<LabelEntry> labels = List.of(
            new LabelEntry("musht", "tilapia", "musht", 10, 15)
        );
        LabeledSentence sentence = new LabeledSentence("http://example.com/f1", "http://example.com/topic", "en", "I caught musht", labels);
        
        assertEquals("musht", sentence.labels().get(0).surface());
        assertEquals("tilapia", sentence.labels().get(0).canonical());
        assertEquals("musht", sentence.labels().get(0).variant());
    }
}
