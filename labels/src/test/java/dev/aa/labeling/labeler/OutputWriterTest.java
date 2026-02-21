package dev.aa.labeling.labeler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class OutputWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void testWriteData() throws Exception {
        OutputWriter writer = new OutputWriter(tempDir, "output.jsonl");
        writer.open();
        
        LabeledSentence sentence = new LabeledSentence(
            "http://example.com/forum1",
            "http://example.com/topic", 
            "ru", 
            "Я поймал карпа", 
            List.of(new LabelEntry("карпа", "карп", null, 7, 12, true))
        );
        writer.writeData(sentence);
        writer.close();
        
        Path outputFile = tempDir.resolve("output.jsonl");
        assertTrue(Files.exists(outputFile));
        
        String content = Files.readString(outputFile);
        assertTrue(content.contains("http://example.com/topic"));
        assertTrue(content.contains("http://example.com/forum1"));
        assertTrue(content.contains("ru"));
        assertTrue(content.contains("карпа"));
    }

    @Test
    void testWriteMultipleSentences() throws Exception {
        OutputWriter writer = new OutputWriter(tempDir, "output.jsonl");
        writer.open();
        
        writer.writeData(new LabeledSentence("http://example.com/f1", "http://example.com/1", "ru", "text1", List.of(new LabelEntry("a", "a", null, 0, 1, true))));
        writer.writeData(new LabeledSentence("http://example.com/f1", "http://example.com/2", "en", "text2", List.of(new LabelEntry("b", "b", null, 0, 1, true))));
        writer.close();
        
        Path outputFile = tempDir.resolve("output.jsonl");
        String content = Files.readString(outputFile);
        
        assertTrue(content.contains("http://example.com/1"));
        assertTrue(content.contains("http://example.com/2"));
    }

    @Test
    void testWriteResult() throws Exception {
        List<LabeledSentence> sentences = List.of(
            new LabeledSentence("http://example.com/f1", "http://example.com/topic", "ru", "Предложение 1", List.of(new LabelEntry("label1", "label1", null, 0, 6, true))),
            new LabeledSentence("http://example.com/f1", "http://example.com/topic", "ru", "Предложение 2", List.of(new LabelEntry("label2", "label2", null, 0, 6, true)))
        );
        LabelingMetadata metadata = new LabelingMetadata("ru", "2026-01-01", 2, 10, 1);
        LabelingResult result = new LabelingResult(sentences, metadata);
        
        OutputWriter writer = new OutputWriter(tempDir, "result.jsonl");
        writer.write(result);
        
        Path outputFile = tempDir.resolve("result.jsonl");
        assertTrue(Files.exists(outputFile));
        
        String content = Files.readString(outputFile);
        assertTrue(content.contains("Предложение 1"));
        assertTrue(content.contains("Предложение 2"));
    }

    @Test
    void testJsonLinesFormat() throws Exception {
        OutputWriter writer = new OutputWriter(tempDir, "output.jsonl");
        writer.open();
        
        writer.writeData(new LabeledSentence("http://example.com/f1", "http://example.com", "en", "test sentence", List.of(new LabelEntry("test", "test", null, 0, 4, true))));
        writer.close();
        
        Path outputFile = tempDir.resolve("output.jsonl");
        String content = Files.readString(outputFile);
        
        // New format: one JSON per line (pretty-printed)
        assertTrue(content.contains("\"type\" : \"data\""));
        assertTrue(content.contains("\"topicUrl\""));
        assertTrue(content.contains("\"forumUrl\""));
        assertTrue(content.contains("\"lang\""));
        assertTrue(content.contains("\"labels\""));
        // New fields
        assertTrue(content.contains("\"surface\""));
        assertTrue(content.contains("\"canonical\""));
    }

    @Test
    void testWriteMarkers() throws Exception {
        OutputWriter writer = new OutputWriter(tempDir, "output.jsonl");
        writer.open();
        
        writer.writeForumStart("http://example.com/forum1");
        writer.writeTopicStart("http://example.com/forum1", "http://example.com/topic1");
        writer.writeTopicEnd("http://example.com/forum1", "http://example.com/topic1");
        writer.writeForumEnd("http://example.com/forum1");
        writer.close();
        
        Path outputFile = tempDir.resolve("output.jsonl");
        String content = Files.readString(outputFile);
        
        assertTrue(content.contains("\"type\" : \"forum_start\""));
        assertTrue(content.contains("\"type\" : \"topic_start\""));
        assertTrue(content.contains("\"type\" : \"topic_end\""));
        assertTrue(content.contains("\"type\" : \"forum_end\""));
    }

    @Test
    void testCreatesDirectory() throws Exception {
        Path subDir = tempDir.resolve("subdir");
        
        OutputWriter writer = new OutputWriter(subDir, "output.jsonl");
        writer.open();
        writer.writeData(new LabeledSentence("http://example.com/f1", "http://example.com", "en", "test", List.of()));
        writer.close();
        
        assertTrue(Files.exists(subDir.resolve("output.jsonl")));
    }
    
    @Test
    void testVariantOutput() throws Exception {
        OutputWriter writer = new OutputWriter(tempDir, "output.jsonl");
        writer.open();
        
        // Test with variant
        writer.writeData(new LabeledSentence("http://example.com/f1", "http://example.com", "en", "I caught musht", 
            List.of(new LabelEntry("musht", "tilapia", "musht", 9, 14, true))));
        writer.close();
        
        Path outputFile = tempDir.resolve("output.jsonl");
        String content = Files.readString(outputFile);
        
        assertTrue(content.contains("\"canonical\" : \"tilapia\""));
        assertTrue(content.contains("\"variant\" : \"musht\""));
    }
}
