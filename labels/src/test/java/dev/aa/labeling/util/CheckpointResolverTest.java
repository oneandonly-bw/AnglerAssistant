package dev.aa.labeling.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CheckpointResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void testLoadCompleted_EmptyFile() throws Exception {
        Path outputFile = tempDir.resolve("output.jsonl");
        Files.writeString(outputFile, "");
        
        var result = CheckpointResolver.loadCompleted(outputFile);
        
        assertTrue(result.completedTopicUrls().isEmpty());
        assertTrue(result.completedForumUrls().isEmpty());
    }

    @Test
    void testLoadCompleted_NoFile() throws Exception {
        Path outputFile = tempDir.resolve("nonexistent.jsonl");
        
        var result = CheckpointResolver.loadCompleted(outputFile);
        
        assertTrue(result.completedTopicUrls().isEmpty());
    }

    @Test
    void testLoadCompleted_WithCompletedTopics() throws Exception {
        Path outputFile = tempDir.resolve("output.jsonl");
        String content = """
            {"type": "forum_start", "forumUrl": "http://example.com/f1", "timestamp": "2026-02-18T10:00:00Z"}
            {"type": "topic_start", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t1"}
            {"type": "data", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t1", "text": "test", "labels": []}
            {"type": "topic_end", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t1"}
            {"type": "topic_start", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t2"}
            {"type": "data", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t2", "text": "test2", "labels": []}
            {"type": "topic_end", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t2"}
            {"type": "forum_end", "forumUrl": "http://example.com/f1"}
            """;
        Files.writeString(outputFile, content);
        
        var result = CheckpointResolver.loadCompleted(outputFile);
        
        assertEquals(2, result.completedTopicUrls().size());
        assertTrue(result.completedTopicUrls().contains("http://example.com/t1"));
        assertTrue(result.completedTopicUrls().contains("http://example.com/t2"));
        assertTrue(result.completedForumUrls().contains("http://example.com/f1"));
    }

    @Test
    void testFindIncompleteTopics() throws Exception {
        Path outputFile = tempDir.resolve("output.jsonl");
        String content = """
            {"type": "topic_start", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t1"}
            {"type": "data", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t1", "text": "test", "labels": []}
            {"type": "topic_end", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t1"}
            {"type": "topic_start", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t2"}
            {"type": "data", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t2", "text": "test2", "labels": []}
            """;
        Files.writeString(outputFile, content);
        
        var incomplete = CheckpointResolver.findIncompleteTopics(outputFile);
        
        assertEquals(1, incomplete.size());
        assertTrue(incomplete.contains("http://example.com/t2"));
    }

    @Test
    void testCleanupIncomplete() throws Exception {
        Path outputFile = tempDir.resolve("output.jsonl");
        String content = """
            {"type": "forum_start", "forumUrl": "http://example.com/f1", "timestamp": "2026-02-18T10:00:00Z"}
            {"type": "topic_start", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t1"}
            {"type": "data", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t1", "text": "test1", "labels": []}
            {"type": "topic_end", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t1"}
            {"type": "topic_start", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t2"}
            {"type": "data", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t2", "text": "test2", "labels": []}
            {"type": "topic_start", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t3"}
            {"type": "data", "forumUrl": "http://example.com/f1", "topicUrl": "http://example.com/t3", "text": "test3", "labels": []}
            {"type": "forum_end", "forumUrl": "http://example.com/f1"}
            """;
        Files.writeString(outputFile, content);
        
        CheckpointResolver.cleanupIncomplete(outputFile);
        
        var lines = Files.readAllLines(outputFile);
        
        assertTrue(lines.stream().anyMatch(l -> l.contains("forum_start")), "Should keep forum_start");
        assertTrue(lines.stream().anyMatch(l -> l.contains("t1")), "Should keep t1");
        assertFalse(lines.stream().anyMatch(l -> l.contains("t2")), "Should remove t2");
        assertFalse(lines.stream().anyMatch(l -> l.contains("t3")), "Should remove t3");
        assertTrue(lines.stream().anyMatch(l -> l.contains("forum_end")), "Should keep forum_end");
    }

    @Test
    void testLoadCompleted_IgnoresInvalidJson() throws Exception {
        Path outputFile = tempDir.resolve("output.jsonl");
        String content = """
            {"type": "forum_start", "forumUrl": "http://example.com/f1"}
            invalid json line
            {"type": "topic_end", "topicUrl": "http://example.com/t1"}
            {"type": "forum_end", "forumUrl": "http://example.com/f1"}
            """;
        Files.writeString(outputFile, content);
        
        var result = CheckpointResolver.loadCompleted(outputFile);
        
        assertEquals(1, result.completedTopicUrls().size());
        assertTrue(result.completedForumUrls().contains("http://example.com/f1"));
    }
}
