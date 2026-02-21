package dev.aa.labeling.labeler;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class DictionaryEntryTest {

    @Test
    void testConstructor() {
        List<DictValue> values = List.of(
            new DictValue("carp", "CANONICAL")
        );
        DictionaryEntry entry = new DictionaryEntry("uid1", "SPECIES", values);
        
        assertEquals("uid1", entry.uid());
        assertEquals("SPECIES", entry.type());
        assertEquals("carp", entry.getCanonical());
    }

    @Test
    void testGetters() {
        List<DictValue> values = List.of(
            new DictValue("тилапия", "CANONICAL"),
            new DictValue("мушт", "VARIANT")
        );
        DictionaryEntry entry = new DictionaryEntry("species_001", "SPECIES", values);
        
        assertEquals("species_001", entry.uid());
        assertEquals("SPECIES", entry.type());
        assertEquals("тилапия", entry.getCanonical());
    }

    @Test
    void testGetCanonical() {
        List<DictValue> values = List.of(
            new DictValue("musht", "VARIANT"),
            new DictValue("tilapia", "CANONICAL"),
            new DictValue("st. peter's fish", "VARIANT")
        );
        DictionaryEntry entry = new DictionaryEntry("uid1", "SPECIES", values);
        
        assertEquals("tilapia", entry.getCanonical());
    }
}
