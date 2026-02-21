package dev.aa.labeling.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class SimilarityUtilTest {
    
    @Test
    @DisplayName("Should calculate basic Levenshtein similarity")
    void testLevenshteinSimilarity() {
        String content1 = "test content";
        String content2 = "test content";
        
        double similarity = SimilarityUtil.calculateSimilarity(content1, content2, SimilarityUtil.SimilarityType.LEVENSHTEIN);
        assertEquals(1.0, similarity, "Identical content should have perfect similarity");
    }
    
    @Test
    @DisplayName("Should calculate Jaccard similarity")
    void testJaccardSimilarity() {
        String content1 = "hello world";
        String content2 = "hello there";
        
        double similarity = SimilarityUtil.calculateSimilarity(content1, content2, SimilarityUtil.SimilarityType.JACCARD);
        assertTrue(similarity >= 0.0 && similarity <= 1.0, "Similarity should be in valid range");
    }
    
    @Test
    @DisplayName("Should calculate Cosine similarity")
    void testCosineSimilarity() {
        String content1 = "fishing with carp";
        String content2 = "fishing with carps";
        
        double similarity = SimilarityUtil.calculateSimilarity(content1, content2, SimilarityUtil.SimilarityType.COSINE);
        assertTrue(similarity >= 0.0 && similarity <= 1.0, "Similarity should be in valid range");
    }
    
    @Test
    @DisplayName("Should detect duplicates")
    void testDuplicateDetection() {
        String content1 = "test content";
        String content2 = "test content";
        
        boolean isDuplicate = SimilarityUtil.isDuplicate(content1, content2, SimilarityUtil.SimilarityType.LEVENSHTEIN);
        assertTrue(isDuplicate, "Identical content should be duplicate");
    }
    
    @Test
    @DisplayName("Should handle null content")
    void testNullContent() {
        assertThrows(IllegalArgumentException.class, () -> 
            SimilarityUtil.calculateSimilarity(null, "test", SimilarityUtil.SimilarityType.LEVENSHTEIN));
        assertThrows(IllegalArgumentException.class, () -> 
            SimilarityUtil.calculateSimilarity("test", null, SimilarityUtil.SimilarityType.LEVENSHTEIN));
    }
}