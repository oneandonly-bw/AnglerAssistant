package dev.aa.labeling.labeler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aa.labeling.config.LabelerConfiguration;
import dev.aa.labeling.interfaces.IfTopicLabeler;
import dev.aa.labeling.model.Topic;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SentencesLabeler implements IfTopicLabeler, AutoCloseable {
    private final LabelerConfiguration config;
    private final OutputWriter streamWriter;
    private final CacheManager cacheManager;
    private final LLMAdapter llmAdapter;
    private final Lemmatizer lemmatizer;
    private final DictionaryLoader dictionaryLoader;
    private CountersManager countersManager;
    
    private List<DictionaryEntry> dictionary;
    private Set<String> blockedTerms;
    private int rejectedTermsLimit = 1000;
    private LinkedHashMap<String, Long> rejectedTerms;
    private int cacheSaveInterval = 5;
    private int lastCacheSave = 0;
    private LanguageConfig languageConfig;
    
    private final List<LabeledSentence> results = new ArrayList<>();
    private int topicsProcessed = 0;
    private int sentencesProcessed = 0;
    private int labelsAdded = 0;
    private boolean stopped = false;
    
    public SentencesLabeler(LabelerConfiguration config, Path llmConfigDir) throws Exception {
        this(config, null, llmConfigDir);
    }
    
    public SentencesLabeler(LabelerConfiguration config, OutputWriter streamWriter, Path llmConfigDir) throws Exception {
        this(config, streamWriter, llmConfigDir, null);
    }
    
    public SentencesLabeler(LabelerConfiguration config, OutputWriter streamWriter, Path llmConfigDir, Lemmatizer customLemmatizer) throws Exception {
        this.config = config;
        this.streamWriter = streamWriter;
        this.dictionaryLoader = new DictionaryLoader(new ObjectMapper());
        
        String forumName = config.forumName() != null ? config.forumName() : "default";
        Path dataDir = config.dataDirectory() != null ? config.dataDirectory() : Path.of("data");
        Path forumDataDir = dataDir.resolve(forumName);
        
        Path termsPath = forumDataDir.resolve("terms_seen.txt");
        Path lemmasPath = forumDataDir.resolve("lemmas_seen.txt");
        
        this.cacheManager = new CacheManager(termsPath, lemmasPath);
        this.cacheManager.load();
        
        if (llmConfigDir != null) {
            this.llmAdapter = new LLMAdapterImpl(llmConfigDir);
        } else {
            this.llmAdapter = null;
        }
        
        this.lemmatizer = customLemmatizer != null ? customLemmatizer : LemmatizerFactory.createLemmatizer("RU");
        
        System.out.println("Loading dictionary...");
        loadDictionary();
        
        Path blockedTermsPath = deriveBlockedTermsPath(config.dictionaryPaths(), forumDataDir, config.language());
        
        System.out.println("Loading blocked terms...");
        loadBlockedTerms(blockedTermsPath);
        
        if (streamWriter != null) {
            try {
                streamWriter.open();
                
                Path outputDir = config.outputDirectory() != null ? config.outputDirectory() : Path.of("output");
                String countersFileName = "counters.json";
                if (config.outputFileName() != null && !config.outputFileName().isEmpty()) {
                    String baseName = config.outputFileName().replace(".json", "");
                    countersFileName = baseName + "_counters.json";
                }
                this.countersManager = new CountersManager(outputDir, countersFileName);
                
            } catch (IOException e) {
                System.err.println("Failed to open stream writer: " + e.getMessage());
            }
        }
    }
    
    @Override
    public void processTopic(Topic topic) {
        String language = topic.getLanguage();
        if (language == null || language.isEmpty()) {
            language = "RU";
        }
        
        this.languageConfig = LanguageConfig.forLanguage(language);
        
        String content = topic.getCleanedContent();
        if (content == null || content.isEmpty()) {
            content = topic.getContent();
        }
        if (content == null || content.isEmpty()) {
            return;
        }
        
        if (stopped) {
            return;
        }
        
        List<LabeledSentence> topicSentences = processSentences(content, topic.getForumUrl(), topic.getTopicUrl(), language.toLowerCase());
        results.addAll(topicSentences);
        topicsProcessed++;
        
        if (streamWriter != null) {
            for (LabeledSentence sentence : topicSentences) {
                int maxSentences = config.maxSentences();
                if (maxSentences > 0 && sentencesProcessed >= maxSentences) {
                    System.out.println("Max sentences reached: " + maxSentences);
                    stopped = true;
                    break;
                }
                try {
                    streamWriter.writeData(sentence);
                    sentencesProcessed++;
                    if (sentencesProcessed % 10 == 0) {
                        System.out.println("Written " + sentencesProcessed + " sentences... (terms: " + 
                            cacheManager.getTermsSeenCount() + ", lemmas: " + cacheManager.getLemmasSeenCount() + ")");
                    }
                    
                    // Periodic cache save every 5 sentences
                    if (sentencesProcessed - lastCacheSave >= cacheSaveInterval) {
                        saveCache();
                        lastCacheSave = sentencesProcessed;
                    }
                } catch (IOException e) {
                    System.err.println("Error writing sentence: " + e.getMessage());
                }
            }
        }
    }
    
    private List<LabeledSentence> processSentences(String content, String forumUrl, String topicUrl, String lang) {
        List<LabeledSentence> sentences = new ArrayList<>();
        
        content = content.replaceAll("\\s+", " ").trim();
        String[] rawSentences = languageConfig.getSentencePattern().split(content);
        
        int rawCount = rawSentences.length;
        int minLenCount = 0;
        int langFilteredCount = 0;
        int labeledCount = 0;
        int noCandidateCount = 0;
        
        for (String raw : rawSentences) {
            if (raw.length() < config.minSentenceLength()) {
                minLenCount++;
                continue;
            }
            
            raw = raw.trim();
            if (raw.isEmpty()) continue;
            
            if (!languageConfig.isTargetLanguageSentence(raw)) {
                langFilteredCount++;
                continue;
            }
            
            String originalText = raw.replaceAll("\\s+", " ").trim();
            String cleanedText = removePunctuation(originalText);
            String lowerCleaned = cleanedText.toLowerCase();
            
            // Check if any dictionary value found
            boolean hasCandidate = false;
            for (DictionaryEntry entry : dictionary) {
                for (DictValue dv : entry.values()) {
                    if (lowerCleaned.contains(dv.value().toLowerCase())) {
                        hasCandidate = true;
                        break;
                    }
                }
                if (hasCandidate) break;
            }
            
            if (!hasCandidate) {
                noCandidateCount++;
                continue;
            }
            
            List<LabelEntry> foundLabels = findLabels(originalText, cleanedText, true);
            
            if (!foundLabels.isEmpty()) {
                sentences.add(new LabeledSentence(forumUrl, topicUrl, lang, originalText, foundLabels));
                labelsAdded += foundLabels.size();
                labeledCount++;
            }
        }
        
        if (labeledCount == 0 && rawCount > 0) {
            System.out.println("  Topic " + topicUrl.substring(topicUrl.lastIndexOf('=')+1) + ": " + 
                rawCount + " total, " + minLenCount + " short, " + langFilteredCount + " lang, " + noCandidateCount + " no candidate, " + labeledCount + " labeled");
        }
        
        return sentences;
    }
    
    private String removePunctuation(String text) {
        return text.replaceAll("[^\\p{L}\\s]", "");
    }
    
    private List<LabelEntry> findLabels(String originalText, String cleanedText, boolean debug) {
        List<LabelEntry> found = new ArrayList<>();
        String lowerText = cleanedText.toLowerCase();
        
        for (DictionaryEntry entry : dictionary) {
            String canonical = entry.getCanonical();
            if (canonical == null) continue;
            String canonicalLower = canonical.toLowerCase();
            
            for (DictValue dictValue : entry.values()) {
                String value = dictValue.value();
                String valueLower = value.toLowerCase();
                String specificity = dictValue.specificity();
                
                int idx = 0;
                int firstIdx = lowerText.indexOf(valueLower);
                if (firstIdx == -1) {
                    continue;
                }
                
                while ((idx = lowerText.indexOf(valueLower, idx)) != -1) {
                    int end = idx + valueLower.length();
                    
                    // foundWord is EXACTLY the dictionary value found in text
                    String foundWord = valueLower;
                    
                    // Get word boundaries to extract actual surface from original text
                    int wordStart = idx;
                    int wordEnd = end;
                    
                    // Expand to get actual word from original (with original case)
                    while (wordStart > 0 && Character.isLetterOrDigit(cleanedText.charAt(wordStart - 1))) {
                        wordStart--;
                    }
                    while (wordEnd < cleanedText.length() && Character.isLetterOrDigit(cleanedText.charAt(wordEnd))) {
                        wordEnd++;
                    }
                    String surface = cleanedText.substring(wordStart, wordEnd);
                    
                    if (shouldSkip(surface)) {
                        idx = end;
                        continue;
                    }
                    
                    boolean isMatch = false;
                    String surfaceLower = surface.toLowerCase();
                    
                    // Step 8: seenTerms check
                    if (cacheManager.containsTerm(surfaceLower)) {
                        System.out.println("candidate found, key match => '" + surfaceLower + "', '" + valueLower + "'");
                        isMatch = true;
                    } else if (valueLower.equals(surfaceLower)) {
                        // Step 9: Exact match
                        System.out.println("candidate found, key match => '" + surfaceLower + "', '" + valueLower + "'");
                        cacheManager.addTerm(surfaceLower);
                        isMatch = true;
                    } else {
                        // Step 10: Lemma
                        String lemma = getLemma(surfaceLower);
                        // Step 11: Lemma doesn't contain key -> skip
                        if (lemma != null && !lemma.contains(valueLower)) {
                            // skip - lemma doesn't contain key
                        } else if (lemma != null && lemma.length() == valueLower.length()) {
                            // Step 12: Exact match (no suffix added)
                            System.out.println("candidate found, lemma match => '" + lemma + "', '" + valueLower + "'");
                            cacheManager.addTerm(surfaceLower);
                            cacheManager.addLemma(lemma);
                            isMatch = true;
                        } else if (llmAdapter != null) {
                            // Step 13: LLM
                            boolean llmSaysMatch = llmAdapter.isFormOf(surfaceLower, value, "ru");
                            if (llmSaysMatch) {
                                System.out.println("candidate found, LLM match => '" + surfaceLower + "', '" + valueLower + "'");
                                cacheManager.addTerm(surfaceLower);
                                cacheManager.addLemma(lemma);
                                isMatch = true;
                            } else {
                                // Step 14: LLM FALSE - add to skipList
                                System.out.println("candidate found, LLM reject => '" + surfaceLower + "', '" + valueLower + "'");
                                if (rejectedTerms != null) {
                                    rejectedTerms.put(surfaceLower, System.currentTimeMillis());
                                }
                            }
                        }
                    }
                    
                    if (isMatch) {
                        String variant = null;
                        if ("VARIANT".equals(specificity) || "MOSTLY_USED".equals(specificity)) {
                            variant = value;
                        }
                        
                        found.add(new LabelEntry(surface, canonical, variant, wordStart, wordEnd, true));
                        
                        if (countersManager != null) {
                            countersManager.incrementDictionary(value);
                            countersManager.incrementSurface(surface);
                        }
                    }
                    
                    idx = end;
                }
            }
        }
        
        return found;
    }
    
    private String getLemma(String word) {
        return lemmatizer.lemmatize(word.toLowerCase());
    }
    
    private static final Set<Character> PUNCTUATION_SET = Set.of(',', '.', '!', '?', ';', ':', '-', '"', '\'', ')', ']', ' ');

    private boolean isWordBoundary(String text, int index, boolean isStart) {
        if (isStart) {
            if (index == 0) return true;
            char prev = text.charAt(index - 1);
            if (!Character.isLetterOrDigit(prev)) return true;
            if (PUNCTUATION_SET.contains(prev)) return true;
            return false;
        } else {
            if (index >= text.length()) return true;
            char next = text.charAt(index);
            if (!Character.isLetterOrDigit(next)) return true;
            if (PUNCTUATION_SET.contains(next)) return true;
            return false;
        }
    }
    
    private String extractWord(String text, int start, int end) {
        return text.substring(start, end);
    }
    
    private boolean shouldSkip(String word) {
        if (blockedTerms != null && blockedTerms.contains(word)) {
            return true;
        }
        if (rejectedTerms != null) {
            boolean rejected = rejectedTerms.containsKey(word);
            if (!rejected) {
                rejectedTerms.put(word, System.currentTimeMillis());
            }
            return rejected;
        }
        return false;
    }
    
    private void loadDictionary() {
        dictionary = new ArrayList<>();
        List<String> paths = config.dictionaryPaths();
        
        if (paths == null || paths.isEmpty()) {
            System.out.println("No dictionary paths configured");
            return;
        }
        
        dictionary = dictionaryLoader.loadDictionaries(paths, config.language());
        
        System.out.println("Dictionary loaded: " + dictionary.size() + " entries");
    }
    
    private Path deriveBlockedTermsPath(List<String> dictionaryPaths, Path forumDataDir, String language) {
        String lang = language != null ? language.toLowerCase() : null;
        
        if (dictionaryPaths == null || dictionaryPaths.isEmpty()) {
            return null;
        }
        
        String dictPath = dictionaryPaths.get(0);
        Path dictPathObj = Path.of(dictPath);
        String dictFileName = dictPathObj.getFileName().toString();
        
        String blockedFileName = dictFileName
            .replace("_dict.json", "_blocked_terms.txt")
            .replace(".json", "_blocked_terms.txt");
        
        String blockedFileNameWithLang = (lang != null ? lang + "_" : "") + blockedFileName;
        
        Path dictDir = dictPathObj.getParent();
        
        if (dictDir != null) {
            Path blockedPathWithLang = dictDir.resolve(blockedFileNameWithLang);
            if (Files.exists(blockedPathWithLang)) {
                return blockedPathWithLang;
            }
        }
        
        Path forumBlockedPathWithLang = forumDataDir.resolve(blockedFileNameWithLang);
        if (Files.exists(forumBlockedPathWithLang)) {
            return forumBlockedPathWithLang;
        }
        
        Path classpathBlocked = findInClasspath(blockedFileNameWithLang);
        if (classpathBlocked != null) {
            return classpathBlocked;
        }
        
        return null;
    }
    
    private Path findInClasspath(String fileName) {
        var url = getClass().getClassLoader().getResource(fileName);
        if (url != null) {
            try {
                return Path.of(url.toURI());
            } catch (Exception e) {
                return null;
            }
        }
        
        url = getClass().getClassLoader().getResource("dictionaries/" + fileName);
        if (url != null) {
            try {
                return Path.of(url.toURI());
            } catch (Exception e) {
                return null;
            }
        }
        
        return null;
    }
    
    private void loadBlockedTerms(Path blockedTermsPath) {
        blockedTerms = new HashSet<>();
        
        if (blockedTermsPath == null || !Files.exists(blockedTermsPath)) {
            if (blockedTermsPath != null) {
                System.out.println("No blocked terms file: " + blockedTermsPath);
            }
        } else {
            try {
                List<String> lines = Files.readAllLines(blockedTermsPath);
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                        blockedTerms.add(trimmed);
                    }
                }
                System.out.println("Blocked terms loaded: " + blockedTerms.size() + " terms (persistent)");
            } catch (IOException e) {
                System.err.println("Failed to load blocked terms: " + e.getMessage());
            }
        }
        
        rejectedTermsLimit = config.blockedTermsLimit() > 0 ? config.blockedTermsLimit() : 1000;
        rejectedTerms = new LinkedHashMap<String, Long>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Long> eldest) {
                return size() > rejectedTermsLimit;
            }
        };
    }
    
    private void saveCache() {
        try {
            cacheManager.save();
        } catch (IOException e) {
            System.err.println("Error saving cache: " + e.getMessage());
        }
    }
    
    public LabelingResult getResult() {
        LabelingMetadata metadata = new LabelingMetadata(
            "RU",
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            results.size(),
            dictionary.size(),
            topicsProcessed
        );
        
        return new LabelingResult(results, metadata);
    }
    
    public int getCacheTermsCount() {
        return cacheManager.getTermsSeenCount();
    }
    
    public int getCacheLemmasCount() {
        return cacheManager.getLemmasSeenCount();
    }
    
    @Override
    public void close() {
        if (streamWriter != null) {
            try {
                streamWriter.close();
                System.out.println("Stream writer closed. Total sentences: " + sentencesProcessed);
            } catch (IOException e) {
                System.err.println("Error closing stream writer: " + e.getMessage());
            }
        }
        
        try {
            cacheManager.save();
            System.out.println("Cache saved. Terms: " + cacheManager.getTermsSeenCount() + 
                ", Lemmas: " + cacheManager.getLemmasSeenCount());
        } catch (IOException e) {
            System.err.println("Error saving cache: " + e.getMessage());
        }
        
        if (countersManager != null) {
            countersManager.save();
            System.out.println("Counters saved. Dictionary entries: " + countersManager.getDictionaryTotal() + 
                ", Surface forms: " + countersManager.getSurfaceTotal());
        }
        
        if (llmAdapter instanceof LLMAdapterImpl) {
            ((LLMAdapterImpl) llmAdapter).close();
        }
    }
    
    @Override
    public boolean isStopped() {
        return stopped;
    }
}
