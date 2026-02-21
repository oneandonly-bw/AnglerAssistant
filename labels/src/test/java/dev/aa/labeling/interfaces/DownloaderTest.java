package dev.aa.labeling.interfaces;

import dev.aa.labeling.extractors.TopicsListExtractor;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Method;

class DownloaderTest {
    
    @Test
    void testDownloaderInterface() {
        assertTrue(IfDownloader.class.isInterface());
    }
    
    @Test
    void testDownloaderHasRequiredMethods() throws NoSuchMethodException {
        Method m1 = IfDownloader.class.getMethod("bindAdapter", TopicsListExtractor.class);
        assertNotNull(m1);
        
        Method m2 = IfDownloader.class.getMethod("bindExtractor", IfTopicLabeler.class);
        assertNotNull(m2);
        
        Method m3 = IfDownloader.class.getMethod("download");
        assertNotNull(m3);
    }
}
