package dev.aa.labeling.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FingerprintUtilTest {
    
    @Test
    void testComputeFingerprint() {
        long fp1 = FingerprintUtil.computeFingerprint("test content");
        long fp2 = FingerprintUtil.computeFingerprint("test content");
        
        assertEquals(fp1, fp2);
    }
    
    @Test
    void testDifferentContentDifferentFingerprint() {
        long fp1 = FingerprintUtil.computeFingerprint("content 1");
        long fp2 = FingerprintUtil.computeFingerprint("content 2");
        
        assertNotEquals(fp1, fp2);
    }
    
    @Test
    void testNullContent() {
        long fp = FingerprintUtil.computeFingerprint(null);
        assertEquals(0L, fp);
    }
    
    @Test
    void testEmptyContent() {
        long fp = FingerprintUtil.computeFingerprint("");
        assertEquals(0L, fp);
    }
    
    @Test
    void testLongContent() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("test content ");
        }
        long fp = FingerprintUtil.computeFingerprint(sb.toString());
        assertNotEquals(0L, fp);
    }
}
