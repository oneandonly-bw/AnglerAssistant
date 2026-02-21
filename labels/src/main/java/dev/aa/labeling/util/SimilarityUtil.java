package dev.aa.labeling.util;

import org.apache.commons.text.similarity.JaccardSimilarity;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.text.similarity.CosineDistance;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SimilarityUtil {
    
    private static final double DEFAULT_LEVENSHTEIN_THRESHOLD = 0.8;
    private static final double DEFAULT_JACCARD_THRESHOLD = 0.7;
    private static final double DEFAULT_COSINE_THRESHOLD = 0.8;
    
    public enum SimilarityType {
        LEVENSHTEIN,
        JACCARD,
        COSINE
    }
    
    public static class SimilarityResult {
        private final String content1;
        private final String content2;
        private final SimilarityType type;
        
        public SimilarityResult(String content1, String content2, SimilarityType type) {
            this.content1 = content1;
            this.content2 = content2;
            this.type = type;
        }
        
        public String getContent1() {
            return content1;
        }
        
        public String getContent2() {
            return content2;
        }
        
        public SimilarityType getType() {
            return type;
        }
    }
    
    public static double calculateSimilarity(String content1, String content2, SimilarityType type) {
        if (content1 == null || content2 == null) {
            throw new IllegalArgumentException("Content cannot be null");
        }
        
        return switch (type) {
            case LEVENSHTEIN -> calculateLevenshteinSimilarity(content1, content2);
            case JACCARD -> calculateJaccardSimilarity(content1, content2);
            case COSINE -> calculateCosineSimilarity(content1, content2);
        };
    }
    
    public static boolean isDuplicate(String content1, String content2, SimilarityType type) {
        return isDuplicate(content1, content2, type, getDefaultThreshold(type));
    }
    
    public static boolean isDuplicate(String content1, String content2, SimilarityType type, double threshold) {
        double similarity = calculateSimilarity(content1, content2, type);
        return similarity >= threshold;
    }
    
    public static SimilarityResult generateResult(String content1, String content2, SimilarityType type) {
        return new SimilarityResult(content1, content2, type);
    }
    
    private static double calculateLevenshteinSimilarity(String content1, String content2) {
        LevenshteinDistance distance = new LevenshteinDistance();
        int maxLen = Math.max(content1.length(), content2.length());
        
        if (maxLen == 0) {
            return 1.0;
        }
        
        int editDistance = distance.apply(content1, content2);
        return 1.0 - ((double) editDistance / maxLen);
    }
    
    private static double calculateJaccardSimilarity(String content1, String content2) {
        // Convert to character sets for Jaccard
        Set<Character> set1 = new HashSet<>();
        Set<Character> set2 = new HashSet<>();
        
        for (char c : content1.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                set1.add(Character.toLowerCase(c));
            }
        }
        
        for (char c : content2.toCharArray()) {
            if (!Character.isWhitespace(c)) {
                set2.add(Character.toLowerCase(c));
            }
        }
        
        JaccardSimilarity jaccard = new JaccardSimilarity();
        return jaccard.apply(Arrays.toString(set1.toArray()), Arrays.toString(set2.toArray()));
    }
    
    private static double calculateCosineSimilarity(String content1, String content2) {
        CosineDistance cosine = new CosineDistance();
        return cosine.apply(content1, content2);
    }
    
    private static double getDefaultThreshold(SimilarityType type) {
        return switch (type) {
            case LEVENSHTEIN -> DEFAULT_LEVENSHTEIN_THRESHOLD;
            case JACCARD -> DEFAULT_JACCARD_THRESHOLD;
            case COSINE -> DEFAULT_COSINE_THRESHOLD;
        };
    }
}