package dev.aa.labeling.labeler;

import dev.aa.labeling.Constants;
import dev.aa.labeling.config.LabelerConfiguration;
import dev.aa.labeling.model.Topic;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class SentencesLabelerIntegrationTest {

    @TempDir
    Path tempDir;

    @Test
    void testFullProcessingWithMarkers() throws Exception {
        Path llmConfigDir = Path.of(Constants.DEFAULT_LLM_CONFIG_PATH);
        Path outputDir = tempDir.resolve("output");
        Path outputFile = outputDir.resolve("test_valid.jsonl");
        
        LabelerConfiguration config = new LabelerConfiguration(
            true, 15, 200, 0.3, 0.2,
            List.of(Constants.DEFAULT_DICTIONARY_PATH),
            1000,
            tempDir.resolve("data"),
            outputDir,
            "test.jsonl",
            "ru",
            "test_forum",
            0
        );
        
        OutputWriter writer = new OutputWriter(outputDir, "test.jsonl");
        SentencesLabeler labeler = new SentencesLabeler(config, writer, llmConfigDir, new NoOpLemmatizer());
        
        Topic topic = new Topic("test", "Test", "Test", "http://example.com/forum1", "http://example.com/topic1", "1");
        topic.setContent("Я поймал огромного карпа и щуку на рыбалке.");
        topic.setLanguage("RU");
        
        writer.writeForumStart("http://example.com/forum1");
        writer.writeTopicStart("http://example.com/forum1", "http://example.com/topic1");
        
        labeler.processTopic(topic);
        
        writer.writeTopicEnd("http://example.com/forum1", "http://example.com/topic1");
        writer.writeForumEnd("http://example.com/forum1");
        writer.close();
        
        labeler.close();
        
        assertTrue(Files.exists(outputFile));
        
        var lines = Files.readAllLines(outputFile);
        assertTrue(lines.size() >= 5, "Should have at least 5 lines: forum_start, topic_start, data, topic_end, forum_end");
        
        assertTrue(lines.get(0).contains("\"type\" : \"forum_start\""));
        assertTrue(lines.get(0).contains("http://example.com/forum1"));
        
        assertTrue(lines.get(1).contains("\"type\" : \"topic_start\""));
        assertTrue(lines.get(1).contains("http://example.com/topic1"));
        
        boolean hasData = false;
        for (String line : lines) {
            if (line.contains("\"type\" : \"data\"")) {
                hasData = true;
                assertTrue(line.contains("карп") || line.contains("щука"), "Data should contain fish names");
                assertTrue(line.contains("\"labels\""), "Data should have labels array");
                break;
            }
        }
        assertTrue(hasData, "Should have at least one data entry");
        
        boolean hasTopicEnd = false;
        for (String line : lines) {
            if (line.contains("\"type\" : \"topic_end\"")) {
                hasTopicEnd = true;
                break;
            }
        }
        assertTrue(hasTopicEnd, "Should have topic_end marker");
        
        assertTrue(lines.get(lines.size() - 1).contains("\"type\" : \"forum_end\""));
    }

    @Test
    void testMultipleTopicsProcessing() throws Exception {
        Path llmConfigDir = Path.of(Constants.DEFAULT_LLM_CONFIG_PATH);
        Path outputDir = tempDir.resolve("output");
        
        LabelerConfiguration config = new LabelerConfiguration(
            true, 15, 200, 0.3, 0.2,
            List.of(Constants.DEFAULT_DICTIONARY_PATH),
            1000,
            tempDir.resolve("data"),
            outputDir,
            "test.jsonl",
            "ru",
            "test_forum",
            0
        );
        
        OutputWriter writer = new OutputWriter(outputDir, "test.jsonl");
        SentencesLabeler labeler = new SentencesLabeler(config, writer, llmConfigDir, new NoOpLemmatizer());
        
        writer.writeForumStart("http://example.com/forum1");
        
        Topic topic1 = new Topic("test", "Test", "Test", "http://example.com/forum1", "http://example.com/topic1", "1");
        topic1.setContent("Поймал сома на рыбалке.");
        topic1.setLanguage("RU");
        
        writer.writeTopicStart("http://example.com/forum1", "http://example.com/topic1");
        labeler.processTopic(topic1);
        writer.writeTopicEnd("http://example.com/forum1", "http://example.com/topic1");
        
        Topic topic2 = new Topic("test", "Test", "Test", "http://example.com/forum1", "http://example.com/topic2", "2");
        topic2.setContent("Я поймал карпа на червя.");
        topic2.setLanguage("RU");
        
        writer.writeTopicStart("http://example.com/forum1", "http://example.com/topic2");
        labeler.processTopic(topic2);
        writer.writeTopicEnd("http://example.com/forum1", "http://example.com/topic2");
        
        writer.writeForumEnd("http://example.com/forum1");
        writer.close();
        
        labeler.close();
        
        Path outputFile = outputDir.resolve("test_valid.jsonl");
        var lines = Files.readAllLines(outputFile);
        
        long topicStarts = lines.stream().filter(l -> l.contains("\"type\" : \"topic_start\"")).count();
        long topicEnds = lines.stream().filter(l -> l.contains("\"type\" : \"topic_end\"")).count();
        
        assertEquals(2, topicStarts, "Should have 2 topic_start markers");
        assertEquals(2, topicEnds, "Should have 2 topic_end markers");
    }

    @Test
    void testCacheFilesCreated() throws Exception {
        Path llmConfigDir = Path.of(Constants.DEFAULT_LLM_CONFIG_PATH);
        Path outputDir = tempDir.resolve("output2");
        
        LabelerConfiguration config = new LabelerConfiguration(
            true, 15, 200, 0.3, 0.2,
            List.of(Constants.DEFAULT_DICTIONARY_PATH),
            1000,
            tempDir.resolve("data"),
            outputDir,
            "test.jsonl",
            "ru",
            "test_forum",
            0
        );
        
        OutputWriter writer = new OutputWriter(outputDir, "test.jsonl");
        SentencesLabeler labeler = new SentencesLabeler(config, writer, llmConfigDir, new NoOpLemmatizer());
        
        Topic topic = new Topic("test", "Test", "Test", "http://example.com/forum1", "http://example.com/topic1", "1");
        topic.setContent("Я поймал карпа на червя.");
        topic.setLanguage("RU");
        
        labeler.processTopic(topic);
        
        writer.close();
        labeler.close();
        
        Path forumDataDir = tempDir.resolve("data").resolve("test_forum");
        assertTrue(Files.exists(forumDataDir), "Forum data directory should exist");
        
        Path termsFile = forumDataDir.resolve("terms_seen.txt");
        Path lemmasFile = forumDataDir.resolve("lemmas_seen.txt");
        
        assertTrue(Files.exists(termsFile), "terms_seen.txt should be created");
        assertTrue(Files.exists(lemmasFile), "lemmas_seen.txt should be created");
        
        var termsContent = Files.readString(termsFile);
        var lemmasContent = Files.readString(lemmasFile);
        
        assertFalse(termsContent.isEmpty(), "terms_seen.txt should not be empty");
        assertTrue(termsContent.contains("карп"), "terms_seen.txt should contain 'карп'");
    }

    @Test
    void testLongSentenceContextExtraction() throws Exception {
        Path llmConfigDir = Path.of(Constants.DEFAULT_LLM_CONFIG_PATH);
        Path outputDir = tempDir.resolve("output_long");
        
        LabelerConfiguration config = new LabelerConfiguration(
            true, 15, 50, 0.3, 0.2,
            List.of(Constants.DEFAULT_DICTIONARY_PATH),
            1000,
            tempDir.resolve("data"),
            outputDir,
            "test.jsonl",
            "ru",
            "test_forum",
            0
        );
        
        OutputWriter writer = new OutputWriter(outputDir, "test.jsonl");
        SentencesLabeler labeler = new SentencesLabeler(config, writer, llmConfigDir, new NoOpLemmatizer());
        
        Topic topic = new Topic("test", "Test", "Test", "http://example.com/forum1", "http://example.com/topic1", "1");
        topic.setContent("Вчера я ходил на рыбалку и поймал огромного карпа который весил около пяти килограмм а также несколько небольших карасей.");
        topic.setLanguage("RU");
        
        writer.writeForumStart("http://example.com/forum1");
        writer.writeTopicStart("http://example.com/forum1", "http://example.com/topic1");
        
        labeler.processTopic(topic);
        
        writer.writeTopicEnd("http://example.com/forum1", "http://example.com/topic1");
        writer.writeForumEnd("http://example.com/forum1");
        writer.close();
        
        labeler.close();
        
        Path outputFile = outputDir.resolve("test_valid.jsonl");
        var lines = Files.readAllLines(outputFile);
        
        long dataLines = lines.stream()
            .filter(l -> l.contains("\"type\" : \"data\""))
            .count();
        
        assertTrue(dataLines >= 1, "Should have at least one data line");
        
        boolean hasContextMarker = false;
        Pattern textPattern = Pattern.compile("\"text\"\\s*:\\s*\"([^\"]+)\"");
        for (String line : lines) {
            if (line.contains("topic1(context)")) {
                hasContextMarker = true;
                assertTrue(line.contains("карп"), "Context should contain the fish name");
                Matcher m = textPattern.matcher(line);
                if (m.find()) {
                    String text = m.group(1);
                    assertTrue(text.length() < 100, "Context text should be shorter than original (~120 chars)");
                }
                break;
            }
        }
        assertTrue(hasContextMarker, "Should have context marker in topicUrl");
    }

    @Test
    void testLongSentenceWithMultipleLabels() throws Exception {
        Path llmConfigDir = Path.of(Constants.DEFAULT_LLM_CONFIG_PATH);
        Path outputDir = tempDir.resolve("output_multi");
        
        LabelerConfiguration config = new LabelerConfiguration(
            true, 15, 60, 0.3, 0.2,
            List.of(Constants.DEFAULT_DICTIONARY_PATH),
            1000,
            tempDir.resolve("data"),
            outputDir,
            "test.jsonl",
            "ru",
            "test_forum",
            0
        );
        
        OutputWriter writer = new OutputWriter(outputDir, "test.jsonl");
        SentencesLabeler labeler = new SentencesLabeler(config, writer, llmConfigDir, new NoOpLemmatizer());
        
        Topic topic = new Topic("test", "Test", "Test", "http://example.com/forum1", "http://example.com/topic1", "1");
        topic.setContent("Вчера на рыбалке я поймал огромного карпа и сазана, а потом еще карпа поймал в озере.");
        topic.setLanguage("RU");
        
        writer.writeForumStart("http://example.com/forum1");
        writer.writeTopicStart("http://example.com/forum1", "http://example.com/topic1");
        
        labeler.processTopic(topic);
        
        writer.writeTopicEnd("http://example.com/forum1", "http://example.com/topic1");
        writer.writeForumEnd("http://example.com/forum1");
        writer.close();
        
        labeler.close();
        
        Path outputFile = outputDir.resolve("test_valid.jsonl");
        var lines = Files.readAllLines(outputFile);
        
        long dataLines = lines.stream()
            .filter(l -> l.contains("\"type\" : \"data\""))
            .count();
        
        assertTrue(dataLines >= 2, "Should have at least 2 data lines (карп x2, сазан)");
    }
}
