package dev.aa.labeling.labeler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CountersManagerTest {
    
    @TempDir
    Path tempDir;
    
    private CountersManager countersManager;
    
    @BeforeEach
    void setUp() {
        countersManager = new CountersManager(tempDir, "test_counters.json");
    }
    
    @Test
    void testInitialCountersAreZero() {
        assertEquals(0, countersManager.getDictionaryTotal());
        assertEquals(0, countersManager.getSurfaceTotal());
    }
    
    @Test
    void testIncrementDictionary() {
        countersManager.incrementDictionary("карп");
        countersManager.incrementDictionary("карп");
        countersManager.incrementDictionary("тилапия");
        
        assertEquals(3, countersManager.getDictionaryTotal());
    }
    
    @Test
    void testIncrementSurface() {
        countersManager.incrementSurface("карпы");
        countersManager.incrementSurface("карпы");
        countersManager.incrementSurface("мушты");
        
        assertEquals(3, countersManager.getSurfaceTotal());
    }
    
    @Test
    void testSaveCreatesFile() throws IOException {
        countersManager.incrementDictionary("карп");
        countersManager.incrementSurface("карпы");
        
        countersManager.save();
        
        Path savedFile = countersManager.getCountersPath();
        assertTrue(Files.exists(savedFile));
        
        String content = Files.readString(savedFile);
        assertTrue(content.contains("\"dictionary\""));
        assertTrue(content.contains("\"text\""));
        assertTrue(content.contains("карп"));
        assertTrue(content.contains("карпы"));
    }
    
    @Test
    void testSaveContainsCorrectCounts() throws IOException {
        countersManager.incrementDictionary("карп");
        countersManager.incrementDictionary("карп");
        countersManager.incrementDictionary("тилапия");
        
        countersManager.incrementSurface("карпы");
        countersManager.incrementSurface("карпы");
        countersManager.incrementSurface("мушты");
        
        countersManager.save();
        
        Path savedFile = countersManager.getCountersPath();
        String content = Files.readString(savedFile);
        
        assertTrue(content.contains("\"value\" : \"карп\""));
        assertTrue(content.contains("\"found\" : 2"));
        assertTrue(content.contains("\"value\" : \"тилапия\""));
        assertTrue(content.contains("\"found\" : 1"));
        assertTrue(content.contains("\"value\" : \"карпы\""));
        assertTrue(content.contains("\"value\" : \"мушты\""));
    }
    
    @Test
    void testUniquePathGeneration() {
        CountersManager first = new CountersManager(tempDir, "unique_test.json");
        first.incrementDictionary("test");
        first.save();
        
        Path firstPath = first.getCountersPath();
        assertTrue(Files.exists(firstPath));
        
        CountersManager second = new CountersManager(tempDir, "unique_test.json");
        second.incrementDictionary("test");
        second.save();
        
        Path secondPath = second.getCountersPath();
        assertTrue(Files.exists(secondPath));
        
        assertNotEquals(firstPath.getFileName(), secondPath.getFileName());
    }
    
    @Test
    void testPathContainsTimestamp() {
        countersManager.incrementDictionary("test");
        countersManager.save();
        
        String fileName = countersManager.getCountersPath().getFileName().toString();
        assertTrue(fileName.matches(".*_\\d{4}-\\d{2}-\\d{2}_\\d{2}-\\d{2}-\\d{2}\\.json$"));
    }
}
