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

PRIORITY:
1. First, verify Candidate is a noun only.
   - Answer FALSE if Candidate is a verb.
   - Answer FALSE if Candidate is an adjective.
   - Answer FALSE if Candidate is an adverb.
   - Only proceed if Candidate is a noun.
2. Then, check if Candidate is a form of Base:
   - a grammatical inflection of Base (nominative, genitive, dative, accusative, instrumental, prepositional)
   - a diminutive, augmentative, or colloquial form of Base
   - a size-modified form of Base

Only answer TRUE if both conditions are satisfied.
Answer FALSE otherwise.
""";
    
    
    /** Russian-specific prompt for LLM-based word form validation */
    public static final String RU_IS_FORM_OF_PROMPT = """
You are a precise %s linguist of Russian. Answer only TRUE or FALSE.

Candidate: %s
Base: %s

Examples of TRUE (same word, different form):
- сома → сом: TRUE (genitive case)
- сомы → сом: TRUE (plural)
- сомик → сом: TRUE (diminutive)
- сомов → сом: TRUE (genitive plural)
- сомище → сом: TRUE (augmentative)

Examples of FALSE (different word type or not related):
- сомячий → сом: FALSE (adjective)
- сомячей → сом: FALSE (adjective)
- сомовьими → сом: FALSE (adjective)
- сомневается → сом: FALSE (verb)
- сомнение → сом: FALSE (candidate is not a form of Base)

Now evaluate:
If Candidate is a noun form of Base → TRUE
Otherwise → FALSE
""";
    
    
    /** Prompt for LLM-based duality validation (temperature 0.1) */
    public static final String DUALITY_CHECK_PROMPT = 
        "You are a precise %s classifier.\nAnswer only TRUE or FALSE.\n\n" +
        "Context: \"%s\"\nTerm: \"%s\"\n\n" +
        "PRIORITY:\n" +
        "Evaluate ONLY whether the Term refers to a %s in this context.\n" +
        "Ignore all other possible meanings.\n\n" +
        "If it refers to a %s, answer TRUE.\n" +
        "If it does not, answer FALSE.";
    
    public static String buildDualityCheckPrompt(String entryType, String context, String term) {
        return String.format(DUALITY_CHECK_PROMPT, entryType, context, term, entryType, entryType);
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
