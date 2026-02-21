package dev.aa.labeling.labeler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CacheManagerUnitTest {

    @TempDir
    Path tempDir;

    @Test
    void testContainsTerm() {
        Path termsPath = tempDir.resolve("terms.txt");
        Path lemmasPath = tempDir.resolve("lemmas.txt");
        
        CacheManager manager = new CacheManager(termsPath, lemmasPath);
        
        assertFalse(manager.containsTerm("карп"));
        
        manager.addTerm("карп");
        
        assertTrue(manager.containsTerm("карп"));
    }

    @Test
    void testContainsLemma() {
        Path termsPath = tempDir.resolve("terms.txt");
        Path lemmasPath = tempDir.resolve("lemmas.txt");
        
        CacheManager manager = new CacheManager(termsPath, lemmasPath);
        
        assertFalse(manager.containsLemma("карп"));
        
        manager.addLemma("карп");
        
        assertTrue(manager.containsLemma("карп"));
    }

    @Test
    void testSaveAndLoad() throws Exception {
        Path termsPath = tempDir.resolve("terms.txt");
        Path lemmasPath = tempDir.resolve("lemmas.txt");
        
        CacheManager manager = new CacheManager(termsPath, lemmasPath);
        manager.addTerm("term1");
        manager.addTerm("term2");
        manager.addLemma("lemma1");
        manager.save();
        
        CacheManager manager2 = new CacheManager(termsPath, lemmasPath);
        manager2.load();
        
        assertEquals(2, manager2.getTermsSeenCount());
        assertEquals(1, manager2.getLemmasSeenCount());
        assertTrue(manager2.containsTerm("term1"));
        assertTrue(manager2.containsLemma("lemma1"));
    }

    @Test
    void testLoadNonExistentFile() throws Exception {
        Path termsPath = tempDir.resolve("nonexistent.txt");
        Path lemmasPath = tempDir.resolve("nonexistent2.txt");
        
        CacheManager manager = new CacheManager(termsPath, lemmasPath);
        manager.load();
        
        assertEquals(0, manager.getTermsSeenCount());
        assertEquals(0, manager.getLemmasSeenCount());
    }
}
