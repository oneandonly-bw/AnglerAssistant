package dev.aa.labeling.labeler;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LanguageConfigTest {

    @Test
    void testForLanguageRu() {
        LanguageConfig config = LanguageConfig.forLanguage("RU");
        assertNotNull(config);
        assertEquals(LanguageConfig.Language.RU, config.getLanguage());
        assertEquals("ru", config.getLanguageCode());
    }

    @Test
    void testForLanguageEn() {
        LanguageConfig config = LanguageConfig.forLanguage("EN");
        assertNotNull(config);
        assertEquals(LanguageConfig.Language.EN, config.getLanguage());
        assertEquals("en", config.getLanguageCode());
    }

    @Test
    void testForLanguageHe() {
        LanguageConfig config = LanguageConfig.forLanguage("HE");
        assertNotNull(config);
        assertEquals(LanguageConfig.Language.HE, config.getLanguage());
        assertEquals("he", config.getLanguageCode());
    }

    @Test
    void testDefaultMinLanguageRatio() {
        LanguageConfig config = LanguageConfig.forLanguage("RU");
        assertEquals(0.3, config.getMinLanguageRatio(), 0.001);
    }

    @Test
    void testCustomMinLanguageRatio() {
        LanguageConfig config = new LanguageConfig(LanguageConfig.Language.RU, 0.5);
        assertEquals(0.5, config.getMinLanguageRatio(), 0.001);
    }

    @Test
    void testIsTargetLanguageSentence_Russian() {
        LanguageConfig config = LanguageConfig.forLanguage("RU");
        assertTrue(config.isTargetLanguageSentence("Привет мир рыбалка"));
        assertTrue(config.isTargetLanguageSentence("карп и щука"));
    }

    @Test
    void testIsTargetLanguageSentence_English() {
        LanguageConfig config = LanguageConfig.forLanguage("EN");
        assertTrue(config.isTargetLanguageSentence("Hello world fishing"));
    }

    @Test
    void testIsTargetLanguageSentence_NonTarget() {
        LanguageConfig config = LanguageConfig.forLanguage("RU");
        assertFalse(config.isTargetLanguageSentence("Hello world"));
    }

    @Test
    void testIsTargetLanguageSentence_Empty() {
        LanguageConfig config = LanguageConfig.forLanguage("RU");
        assertFalse(config.isTargetLanguageSentence(""));
    }

    @Test
    void testWordPatternNotNull() {
        LanguageConfig config = LanguageConfig.forLanguage("RU");
        assertNotNull(config.getWordPattern());
    }

    @Test
    void testSentencePatternNotNull() {
        LanguageConfig config = LanguageConfig.forLanguage("RU");
        assertNotNull(config.getSentencePattern());
    }
}
