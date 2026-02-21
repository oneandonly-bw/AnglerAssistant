package dev.aa.labeling.util;

import dev.aa.labeling.Constants;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConstantsTest {
    
    @Test
    void testDataRoot() {
        assertEquals("data", Constants.DATA_ROOT);
    }
    
    @Test
    void testRawDir() {
        assertEquals("raw", Constants.RAW_DIR);
    }
    
    @Test
    void testProcessedDir() {
        assertEquals("processed", Constants.PROCESSED_DIR);
    }
    
    @Test
    void testConstantsAreNonNull() {
        assertNotNull(Constants.DATA_ROOT);
        assertNotNull(Constants.RAW_DIR);
        assertNotNull(Constants.PROCESSED_DIR);
    }
    
    @Test
    void testDefaultHttpTimeout() {
        assertEquals(30000, Constants.DEFAULT_HTTP_TIMEOUT_MS);
    }
    
    @Test
    void testDefaultRequestDelay() {
        assertEquals(1500, Constants.DEFAULT_REQUEST_DELAY_MS);
    }
}
