package dev.aa.labeling.labeler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LLMAdapterTest {

    @Test
    void testInterfaceExists() {
        LLMAdapter adapter = new TestLLMAdapter();
        assertNotNull(adapter);
    }

    @Test
    void testIsFormOf_Matches() {
        LLMAdapter adapter = new TestLLMAdapter(true);
        assertTrue(adapter.isFormOf("карп", "карпы", "ru"));
        assertTrue(adapter.isFormOf("карп", "карпа", "ru"));
    }

    @Test
    void testIsFormOf_NonMatches() {
        LLMAdapter adapter = new TestLLMAdapter(false);
        assertFalse(adapter.isFormOf("карп", "карпятник", "ru"));
        assertFalse(adapter.isFormOf("карп", "карповик", "ru"));
    }

    private static class TestLLMAdapter implements LLMAdapter {
        private final boolean result;

        TestLLMAdapter() {
            this.result = false;
        }

        TestLLMAdapter(boolean result) {
            this.result = result;
        }

        @Override
        public boolean isFormOf(String key, String candidate, String language) {
            return result;
        }
    }
}
