package dev.aa.labeling;

public class Constants {
    
    // ==================== HTTP Settings ====================
    
    /** HTTP request timeout in milliseconds (used by BaseDownloader, TopicsListExtractor) */
    public static final int DEFAULT_HTTP_TIMEOUT_MS = 30000;
    
    /** Delay between HTTP requests to avoid rate limiting (used by BaseDownloader) */
    public static final int DEFAULT_REQUEST_DELAY_MS = 1500;
    
    
    // ==================== Memory Settings ====================
    
    /** Default memory threshold for JVM (0.0-1.0), used by RuntimeConfiguration */
    public static final double DEFAULT_MEMORY_THRESHOLD = 0.8;
    
    /** Default max retry attempts for failed HTTP requests, used by RuntimeConfiguration */
    public static final int DEFAULT_MAX_RETRIES = 3;
    
    
    // ==================== Labeler Defaults ====================
    
    /** Minimum sentence character length for labeling, used by LabelerConfiguration */
    public static final int DEFAULT_MIN_SENTENCE_LENGTH = 15;
    
    /** Max sentence length before context extraction, used by LabelerConfiguration */
    public static final int DEFAULT_MAX_SENTENCE_LENGTH_FOR_CONTEXT = 200;
    
    /** Minimum % of target language characters (0.0-1.0), used by LabelerConfiguration */
    public static final double DEFAULT_MIN_LANGUAGE_RATIO = 0.3;
    
    /** Maximum % of special characters allowed (0.0-1.0), used by LabelerConfiguration */
    public static final double DEFAULT_MAX_SPECIAL_CHAR_RATIO = 0.2;
    
    /** Minimum similarity threshold for root matching (0.0-1.0), used by SentencesLabeler */
    public static final double DEFAULT_SIMILARITY_THRESHOLD = 0.85;
    
    
    // ==================== Directories ====================
    
    /** Root directory for all data storage (used by PathsManager) */
    public static final String DATA_ROOT = "data";
    
    /** Subdirectory for raw downloaded data (used by PathsManager) */
    public static final String RAW_DIR = "raw";
    
    /** Subdirectory for processed data (future use) */
    public static final String PROCESSED_DIR = "processed";
    
    /** Subdirectory for labeled output data */
    public static final String LABELED_DIR = "labeled";
    
    /** Default output directory for labeled files */
    public static final String OUTPUT_DIR = "output";
    
    
    // ==================== Default Paths ====================
    
    /** Default path to species dictionary JSON file (used by SentencesLabeler) */
    public static final String DEFAULT_DICTIONARY_PATH = "dictionaries/species_dict.json";
    
    /** Default path to forum configuration JSON file (used by Main) */
    public static final String DEFAULT_CONFIG_PATH = "config/forums/israfish.json";
    
    /** Default path to LLM configuration directory */
    public static final String DEFAULT_LLM_CONFIG_PATH = "src/main/resources/llm";
    
    
    // ==================== LLM Settings ====================
    
    /** System prompt for LLM-based word form validation */
    public static final String LLM_PROMPT = """
You are a precise %s linguist. Answer only TRUE or FALSE.

Candidate: %s
Base: %s

TRUE if Candidate is:
  - a grammatical inflection of Base
  - a diminutive or augmentative
  - a colloquial or size-modified form

FALSE if Candidate is:
  - a different %s
  - not a %s
  - not a noun
""";
    
    
    /** Prompt for LLM-based duality validation (temperature 0.1) */
    public static final String DUALITY_CHECK_PROMPT = 
        "You are a precise %s classifier. Always answer only YES or NO. " +
        "\n\nContext: \"%s\"\nTerm: \"%s\" at position %d-%d\nRule: Only answer YES if the Term is a noun and represents a %s in the context. Answer NO otherwise.";
    
    public static String buildDualityCheckPrompt(String entryType, String context, String term, int start, int end) {
        return String.format(DUALITY_CHECK_PROMPT, entryType, context, term, start, end, entryType);
    }
    
    
    // ==================== JSON Output ====================
    
    /** Start delimiter for JSON entries in streaming output (used by OutputWriter) */
    public static final String JSON_ENTRY_START = "-------- Start ----------";
    
    /** End delimiter for JSON entries in streaming output (used by OutputWriter) */
    public static final String JSON_ENTRY_END = "-------- End ------------";
    
    
    // ==================== Lemma Service ====================
    
    /** URL for the lemmatization service */
    public static final String LEMMA_SERVICE_URL = "http://127.0.0.1:5000/lemma";
}
