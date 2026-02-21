package dev.aa.labeling.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class EnumUtilTest {
    
    enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }
    
    @Test
    void testGetValues() {
        List<String> values = EnumUtil.getValues(TestEnum.VALUE1);
        
        assertEquals(3, values.size());
        assertTrue(values.contains("VALUE1"));
        assertTrue(values.contains("VALUE2"));
        assertTrue(values.contains("VALUE3"));
    }
    
    @Test
    void testGetValuesNull() {
        List<String> values = EnumUtil.getValues(null);
        
        assertNull(values);
    }
    
    @Test
    void testGetValuesSingle() {
        enum SingleEnum { ONLY_ONE }
        
        List<String> values = EnumUtil.getValues(SingleEnum.ONLY_ONE);
        
        assertEquals(1, values.size());
        assertEquals("ONLY_ONE", values.get(0));
    }
}
