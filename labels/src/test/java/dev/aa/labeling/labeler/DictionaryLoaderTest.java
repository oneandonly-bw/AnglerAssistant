package dev.aa.labeling.labeler;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DictionaryLoaderTest {

    @Test
    void testLoadDictionaries_EmptyPath() {
        DictionaryLoader loader = new DictionaryLoader(new ObjectMapper());
        List<DictionaryEntry> dictionary = loader.loadDictionaries(List.of(), null);
        
        assertTrue(dictionary.isEmpty());
    }

    @Test
    void testLoadDictionaries_FileNotFound() {
        DictionaryLoader loader = new DictionaryLoader(new ObjectMapper());
        
        assertThrows(RuntimeException.class, () -> 
            loader.loadDictionaries(List.of("nonexistent.json"), "ru")
        );
    }

    @Test
    void testLoadDictionaries_ValidFile() {
        DictionaryLoader loader = new DictionaryLoader(new ObjectMapper());
        List<DictionaryEntry> dictionary = loader.loadDictionaries(
            List.of("dictionaries/species_dict.json"), "ru"
        );
        
        assertFalse(dictionary.isEmpty());
        assertNotNull(dictionary.get(0).getCanonical());
    }

    @Test
    void testLoadDictionaries_MultipleFiles() {
        DictionaryLoader loader = new DictionaryLoader(new ObjectMapper());
        List<DictionaryEntry> dictionary = loader.loadDictionaries(
            List.of(
                "dictionaries/species_dict.json",
                "dictionaries/methods_dict.json"
            ), "ru"
        );
        
        assertFalse(dictionary.isEmpty());
    }

    @Test
    void testLoadDictionaries_FindsCanonical() {
        DictionaryLoader loader = new DictionaryLoader(new ObjectMapper());
        List<DictionaryEntry> dictionary = loader.loadDictionaries(
            List.of("dictionaries/species_dict.json"), "ru"
        );
        
        boolean foundCanonical = dictionary.stream()
            .anyMatch(e -> "тилапия".equals(e.getCanonical()));
        assertTrue(foundCanonical);
    }
}
