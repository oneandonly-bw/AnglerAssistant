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
            tempDir.resolve("data"),
            outputDir,
            "test.json",
            "ru",
            "test_forum",
            0,
            null
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
            System.out.println("Valid Labels: " + sentence.validLabels());
            System.out.println("Invalid Labels: " + sentence.invalidLabels());
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
            tempDir.resolve("data"),
            outputDir,
            "test.json",
            "ru",
            "test_forum",
            0,
            null
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
}
