package dev.aa.labeling.labeler;

import dev.aa.labeling.Constants;
import dev.aa.labeling.config.LabelerConfiguration;
import dev.aa.labeling.model.Topic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SentencesLabelerE2ETest {

    @TempDir
    Path tempDir;

    @Test
    void testLLMIntegration() throws Exception {
        Path llmConfigDir = Path.of(Constants.DEFAULT_LLM_CONFIG_PATH);
        Path outputDir = tempDir.resolve("output");
        
        LabelerConfiguration config = new LabelerConfiguration(
            true, 15, 200, 0.3, 0.2,
            List.of(Constants.DEFAULT_DICTIONARY_PATH),
            1000,
            tempDir.resolve("data"),
            outputDir,
            "test.json",
            "ru",
            "test_forum",
            0
        );
        
        SentencesLabeler labeler = new SentencesLabeler(config, null, llmConfigDir, new NoOpLemmatizer());
        
        Topic topic = new Topic("test", "Test", "Test", "http://example.com/forum1", "http://example.com/topic1", "1");
        topic.setContent("Я поймал огромного сома и маленьких карасиков.");
        topic.setLanguage("RU");
        
        labeler.processTopic(topic);
        
        var result = labeler.getResult();
        
        System.out.println("Sentences found: " + result.getTotalSentences());
        
        for (var sentence : result.sentences()) {
            System.out.println("Sentence: " + sentence.text());
            System.out.println("Labels: " + sentence.labels());
        }
        
        labeler.close();
        
        assertTrue(result.getTotalSentences() > 0, "Should find at least one sentence with labels");
    }

    @Test
    void testLLMRejectsDerivedWords() throws Exception {
        Path llmConfigDir = Path.of(Constants.DEFAULT_LLM_CONFIG_PATH);
        Path outputDir = tempDir.resolve("output");
        
        LabelerConfiguration config = new LabelerConfiguration(
            true, 15, 200, 0.3, 0.2,
            List.of(Constants.DEFAULT_DICTIONARY_PATH),
            1000,
            tempDir.resolve("data"),
            outputDir,
            "test.json",
            "ru",
            "test_forum",
            0
        );
        
        SentencesLabeler labeler = new SentencesLabeler(config, null, llmConfigDir, new NoOpLemmatizer());
        
        Topic topic = new Topic("test", "Test", "Test", "http://example.com/forum1", "http://example.com/topic2", "1");
        topic.setContent("Карпятник ловит карпов.");
        topic.setLanguage("RU");
        
        labeler.processTopic(topic);
        
        var result = labeler.getResult();
        
        System.out.println("Sentences with 'карпятник': " + result.getTotalSentences());
        
        labeler.close();
        
        assertTrue(result.getTotalSentences() > 0, "Should find 'карп' in 'карпов'");
    }

    @Test
    void testBlockedTermsCaseSensitive() throws Exception {
        Path llmConfigDir = Path.of(Constants.DEFAULT_LLM_CONFIG_PATH);
        Path outputDir = tempDir.resolve("output");
        Path dataDir = tempDir.resolve("data");
        Path blockedFile = dataDir.resolve("test_forum").resolve("ru_species_blocked_terms.txt");
        Files.createDirectories(blockedFile.getParent());
        Files.writeString(blockedFile, "Карп\n");
        
        LabelerConfiguration config = new LabelerConfiguration(
            true, 15, 200, 0.3, 0.2,
            List.of(Constants.DEFAULT_DICTIONARY_PATH),
            1000,
            tempDir.resolve("data"),
            outputDir,
            "test.json",
            "ru",
            "test_forum",
            0
        );
        
        SentencesLabeler labeler = new SentencesLabeler(config, null, llmConfigDir, new NoOpLemmatizer());
        
        Topic topic1 = new Topic("test", "Test", "Test", "http://example.com/forum1", "http://example.com/topic3", "1");
        topic1.setContent("Карп клюнул на кукурузу.");
        topic1.setLanguage("RU");
        
        labeler.processTopic(topic1);
        
        var result1 = labeler.getResult();
        
        labeler.close();
        
        assertEquals(0, result1.getTotalSentences(), 
            "Should skip 'Карп' (capitalized, human name) - blocked terms are case-sensitive");
        
        SentencesLabeler labeler2 = new SentencesLabeler(config, null, llmConfigDir, new NoOpLemmatizer());
        
        Topic topic2 = new Topic("test", "Test", "Test", "http://example.com/forum1", "http://example.com/topic4", "1");
        topic2.setContent("Я поймал карпа на червя.");
        topic2.setLanguage("RU");
        
        labeler2.processTopic(topic2);
        
        var result2 = labeler2.getResult();
        
        labeler2.close();
        
        assertTrue(result2.getTotalSentences() > 0, 
            "Should find 'карп' (lowercase, fish) - not blocked");
    }
}
