package dev.aa.labeling.llm;

import dev.aa.labeling.labeler.CacheManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CacheManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void testLoadSave() throws Exception {
        Path termsPath = tempDir.resolve("terms.txt");
        Path lemmasPath = tempDir.resolve("lemmas.txt");
        
        CacheManager manager = new CacheManager(termsPath, lemmasPath);
        
        manager.addTerm("карп");
        manager.addTerm("карась");
        manager.addLemma("карп");
        
        manager.save();
        
        CacheManager manager2 = new CacheManager(termsPath, lemmasPath);
        manager2.load();
        
        assertTrue(manager2.containsTerm("карп"));
        assertTrue(manager2.containsTerm("карась"));
        assertTrue(manager2.containsLemma("карп"));
        assertFalse(manager2.containsTerm("щука"));
    }

    @Test
    void testLoadNonExistent() throws Exception {
        Path termsPath = tempDir.resolve("nonexistent.txt");
        Path lemmasPath = tempDir.resolve("nonexistent2.txt");
        
        CacheManager manager = new CacheManager(termsPath, lemmasPath);
        manager.load();
        
        assertEquals(0, manager.getTermsSeenCount());
        assertEquals(0, manager.getLemmasSeenCount());
    }

    @Test
    void testAddAndContains() {
        Path termsPath = tempDir.resolve("terms.txt");
        Path lemmasPath = tempDir.resolve("lemmas.txt");
        
        CacheManager manager = new CacheManager(termsPath, lemmasPath);
        
        assertFalse(manager.containsTerm("test"));
        assertFalse(manager.containsLemma("test"));
        
        manager.addTerm("test");
        manager.addLemma("lemma");
        
        assertTrue(manager.containsTerm("test"));
        assertTrue(manager.containsLemma("lemma"));
    }
}
