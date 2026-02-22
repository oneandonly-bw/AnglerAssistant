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
        
        content = normalizeText(content);
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
            
            String originalText = raw.trim();
            String cleanedText = originalText;
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
                List<LabelEntry> validLabels = foundLabels.stream()
                    .filter(LabelEntry::isValid)
                    .toList();
                List<LabelEntry> invalidLabels = foundLabels.stream()
                    .filter(l -> !l.isValid())
                    .toList();
                
                int maxContextLength = config.maxSentenceLengthForContext();
                if (originalText.length() > maxContextLength && !validLabels.isEmpty()) {
                    List<LabeledSentence> contextSentences = extractContext(originalText, cleanedText, validLabels, forumUrl, topicUrl, lang);
                    sentences.addAll(contextSentences);
                    labelsAdded += contextSentences.stream().mapToInt(s -> s.validLabels().size()).sum();
                } else {
                    sentences.add(new LabeledSentence(forumUrl, topicUrl, lang, originalText, validLabels, invalidLabels));
                    labelsAdded += foundLabels.size();
                }
                labeledCount++;
                if (labeledCount % 10 == 0) {
                    System.out.println("Labeled so far: " + labeledCount);
                }
            }
        }
        
        if (labeledCount == 0 && rawCount > 0) {
            System.out.println("  Topic " + topicUrl.substring(topicUrl.lastIndexOf('=')+1) + ": " + 
                rawCount + " total, " + minLenCount + " short, " + langFilteredCount + " lang, " + noCandidateCount + " no candidate, " + labeledCount + " labeled");
        }
        
        return sentences;
    }
    
    private List<LabeledSentence> extractContext(String originalText, String cleanedText, List<LabelEntry> validLabels, String forumUrl, String topicUrl, String lang) {
        List<LabeledSentence> contexts = new ArrayList<>();
        String contextSource = topicUrl + "(context)";
        
        String[] words = cleanedText.split("\\s+");
        
        for (LabelEntry label : validLabels) {
            int labelStart = label.start();
            int labelEnd = label.end();
            
            int wordIndex = getWordIndexAtPosition(cleanedText, labelStart);
            
            if (wordIndex >= 0 && wordIndex < words.length) {
                int contextStart = Math.max(0, wordIndex - 5);
                int contextEnd = Math.min(words.length, wordIndex + 6);
                
                if (contextStart < contextEnd) {
                    StringBuilder contextBuilder = new StringBuilder();
                    for (int i = contextStart; i < contextEnd; i++) {
                        if (i > contextStart) contextBuilder.append(" ");
                        contextBuilder.append(words[i]);
                    }
                    String contextText = contextBuilder.toString().trim();
                    
                    if (contextText.length() >= config.minSentenceLength()) {
                        List<LabelEntry> singleLabel = List.of(label);
                        contexts.add(new LabeledSentence(forumUrl, contextSource, lang, contextText, singleLabel, List.of()));
                    }
                }
            }
        }
        
        return contexts;
    }
    
    private int getWordIndexAtPosition(String text, int charPosition) {
        if (charPosition <= 0) return 0;
        String prefix = text.substring(0, Math.min(charPosition, text.length()));
        return prefix.split("\\s+").length - 1;
    }
    
    private String normalizeText(String text) {
        return text.replaceAll("[\r\n]", " ")
                   .replaceAll("([.,!?;:])([^\s])", "$1 $2")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    private List<Candidate> getCandidates(String sentence) {
        List<Candidate> candidates = new ArrayList<>();
        String lowerSentence = sentence.toLowerCase();
        
        for (DictionaryEntry entry : dictionary) {
            String canonical = entry.getCanonical();
            if (canonical == null) continue;
            
            for (DictValue dictValue : entry.values()) {
                String value = dictValue.value();
                String valueLower = value.toLowerCase();
                
                int idx = 0;
                while ((idx = lowerSentence.indexOf(valueLower, idx)) != -1) {
                    int end = idx + valueLower.length();
                    
                    // Extract surface preserving case
                    int wordStart = idx;
                    int wordEnd = end;
                    
                    while (wordStart > 0 && Character.isLetterOrDigit(sentence.charAt(wordStart - 1))) {
                        wordStart--;
                    }
                    while (wordEnd < sentence.length() && Character.isLetterOrDigit(sentence.charAt(wordEnd))) {
                        wordEnd++;
                    }
                    
                    String surface = sentence.substring(wordStart, wordEnd);
                    candidates.add(new Candidate(surface, wordStart, wordEnd, canonical, dictValue));
                    
                    idx = end;
                }
            }
        }
        
        return candidates;
    }
    
    private List<LabelEntry> findLabels(String originalText, String cleanedText, boolean debug) {
        List<Candidate> candidates = getCandidates(cleanedText);
        return getLabels(candidates, cleanedText);
    }
    
    private List<LabelEntry> getLabels(List<Candidate> candidates, String sentence) {
        List<LabelEntry> found = new ArrayList<>();
        
        for (Candidate candidate : candidates) {
            String surface = candidate.surface();
            String canonical = candidate.canonical();
            String surfaceLower = surface.toLowerCase();
            DictValue dictValue = candidate.dictValue();
            String value = dictValue.value();
            String valueLower = value.toLowerCase();
            Duality duality = dictValue.duality();
            
            // Check duality
            if (duality != null) {
                String rule = duality.rule();
                if (rule == null || !"CASE_SENSITIVE".equals(rule)) {
                    throw new IllegalStateException(
                        "Unsupported duality rule: " + rule + " for term: " + value
                    );
                }
                
                // CASE_SENSITIVE: length same, but case different
                if (surface.length() == value.length() && !surface.equals(value)) {
                    // Ask LLM
                    if (llmAdapter != null) {
                        boolean llmSaysFish = llmAdapter.isFish(surface, sentence);
                        if (llmSaysFish) {
                            found.add(new LabelEntry(surface, canonical, value, candidate.start(), candidate.end(), true));
                            // Don't add to seenTerms for duality
                        } else {
                            found.add(new LabelEntry(surface, canonical, value, candidate.start(), candidate.end(), false));
                            // Don't add to seenTerms
                        }
                    } else {
                        // No LLM, skip
                        found.add(new LabelEntry(surface, canonical, value, candidate.start(), candidate.end(), false));
                    }
                    } else {
                        // Same case or different length - treat as valid (add to cache)
                        found.add(new LabelEntry(surface, canonical, value, candidate.start(), candidate.end(), true));
                        cacheManager.addTerm(surfaceLower);
                    }
                continue;
            }
            
            // Normal validation (no duality)
            if (shouldSkip(surfaceLower)) {
                found.add(new LabelEntry(surface, canonical, value, candidate.start(), candidate.end(), false));
                continue;
            }
            
            boolean isMatch = false;
            
            // Step: seenTerms check
            if (cacheManager.containsTerm(surfaceLower)) {
                isMatch = true;
            } else if (valueLower.equals(surfaceLower)) {
                // Exact match
                cacheManager.addTerm(surfaceLower);
                isMatch = true;
            } else {
                // Lemma
                String lemma = getLemma(surfaceLower);
                if (lemma != null && !lemma.contains(valueLower)) {
                    found.add(new LabelEntry(surface, canonical, value, candidate.start(), candidate.end(), false));
                } else if (lemma != null && lemma.length() == valueLower.length()) {
                    // Exact match (no suffix added)
                    cacheManager.addTerm(surfaceLower);
                    cacheManager.addLemma(lemma);
                    isMatch = true;
                } else if (llmAdapter != null) {
                    // LLM
                    boolean llmSaysMatch = llmAdapter.isFormOf(surfaceLower, value, "ru");
                    if (llmSaysMatch) {
                        cacheManager.addTerm(surfaceLower);
                        cacheManager.addLemma(lemma);
                        isMatch = true;
                    } else {
                        found.add(new LabelEntry(surface, canonical, value, candidate.start(), candidate.end(), false));
                        if (rejectedTerms != null) {
                            rejectedTerms.put(surfaceLower, System.currentTimeMillis());
                        }
                    }
                }
            }
            
            if (isMatch) {
                String variant = null;
                if ("VARIANT".equals(dictValue.specificity()) || "MOSTLY_USED".equals(dictValue.specificity())) {
                    variant = value;
                }
                
                found.add(new LabelEntry(surface, canonical, variant, candidate.start(), candidate.end(), true));
                
                if (countersManager != null) {
                    countersManager.incrementDictionary(value);
                    countersManager.incrementSurface(surface);
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
        if (rejectedTerms != null) {
            return rejectedTerms.containsKey(word);
        }
        return false;
    }
    
    private void addToRejected(String word) {
        if (rejectedTerms != null) {
            rejectedTerms.put(word, System.currentTimeMillis());
        }
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
