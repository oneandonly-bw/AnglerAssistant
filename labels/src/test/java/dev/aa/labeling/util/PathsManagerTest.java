package dev.aa.labeling.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PathsManagerTest {
    
    @Test
    @DisplayName("Should get subcategory path")
    void testGetSubcategoryPath() {
        Path expected = Path.of("data", "raw", "israfish", "fresh_water").toAbsolutePath();
        Path actual = PathsManager.getSubcategoryPath("israfish", "fresh_water");
        
        assertEquals(expected, actual);
    }
    
    @Test
    @DisplayName("Should get source path")
    void testGetSourcePath() {
        Path expected = Path.of("data", "raw", "israfish").toAbsolutePath();
        Path actual = PathsManager.getSourcePath("israfish");
        
        assertEquals(expected, actual);
    }
    
    @Test
    @DisplayName("Should ensure directory exists")
    void testEnsureDirectoryExists() {
        Path newDir = Path.of("data", "raw", "israfish", "test_dir_unique");
        
        // Clean up if exists from previous failed test
        try {
            if (Files.exists(newDir)) {
                Files.deleteIfExists(newDir);
            }
        } catch (Exception e) {
            // Ignore cleanup errors
        }
        
        // Ensure directory doesn't exist initially
        assertFalse(Files.exists(newDir));
        
        // Call to method
        PathsManager.ensureDirectoryExists(newDir);
        
        // Directory should now exist
        assertTrue(Files.exists(newDir));
        assertTrue(Files.isDirectory(newDir));
    }
    
    @Test
    @DisplayName("Should validate existing path")
    void testValidatePath() {
        // Create a test directory first
        Path testDir = Path.of("data", "raw");
        PathsManager.ensureDirectoryExists(testDir);
        
        assertDoesNotThrow(() -> {
            PathsManager.validatePath(testDir);
        });
    }
    
    @Test
    @DisplayName("Should throw exception for invalid path")
    void testValidatePathThrows() {
        Path nonExistentPath = Path.of("non", "existent", "path");
        
        assertThrows(RuntimeException.class, () -> {
            PathsManager.validatePath(nonExistentPath);
        });
    }
    
    @Test
    @DisplayName("Should ensure source structure")
    void testEnsureSourceStructure() {
        PathsManager.ensureSourceStructure("israfish");
        
        Path sourcePath = PathsManager.getSourcePath("israfish");
        assertTrue(Files.exists(sourcePath));
        assertTrue(Files.isDirectory(sourcePath));
    }
    
    @Test
    @DisplayName("Should ensure subcategory structure")
    void testEnsureSubcategoryStructure() {
        PathsManager.ensureSubcategoryStructure("israfish", "fresh_water");
        
        Path subcategoryPath = PathsManager.getSubcategoryPath("israfish", "fresh_water");
        assertTrue(Files.exists(subcategoryPath));
        assertTrue(Files.isDirectory(subcategoryPath));
    }
}