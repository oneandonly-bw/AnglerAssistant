package dev.aa.labeling.util;

public class FingerprintUtil {
    
    public static long computeFingerprint(String content) {
        if (content == null || content.isEmpty()) {
            return 0L;
        }
        
        byte[] bytes = content.getBytes();
        long hash = 0x123456789ABCDEFL;
        
        for (byte b : bytes) {
            hash = 31 * hash + (b & 0xFF);
        }
        
        return hash;
    }
}
