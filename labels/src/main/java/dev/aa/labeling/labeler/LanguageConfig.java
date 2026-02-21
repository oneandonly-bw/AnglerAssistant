package dev.aa.labeling.labeler;

import java.util.regex.Pattern;

public class LanguageConfig {

    public enum Language {
        RU("ru", "[.!?]+(?=\\s+[A-ZА-Я]|$)", "[\\u0400-\\u04FF]+"),
        EN("en", "[.!?]+(?=\\s+[A-Z]|$)", "[a-zA-Z]+"),
        HE("he", "[.!?]+(?=\\s+[\\u0590-\\u05FF]|$)", "[\\u0590-\\u05FF]+");

        private final String code;
        private final String sentencePattern;
        private final String wordPattern;

        Language(String code, String sentencePattern, String wordPattern) {
            this.code = code;
            this.sentencePattern = sentencePattern;
            this.wordPattern = wordPattern;
        }

        public String getCode() {
            return code;
        }

        public String getSentencePattern() {
            return sentencePattern;
        }

        public String getWordPattern() {
            return wordPattern;
        }
    }

    private final Language language;
    private final Pattern wordPattern;
    private final Pattern sentencePattern;
    private final double minLanguageRatio;

    public LanguageConfig(Language language, double minLanguageRatio) {
        this.language = language;
        this.wordPattern = Pattern.compile(language.getWordPattern());
        this.sentencePattern = Pattern.compile(language.getSentencePattern());
        this.minLanguageRatio = minLanguageRatio;
    }

    public LanguageConfig(Language language) {
        this(language, 0.3);
    }

    public static LanguageConfig forLanguage(String lang) {
        Language language = Language.valueOf(lang.toUpperCase());
        return new LanguageConfig(language);
    }

    public Language getLanguage() {
        return language;
    }

    public String getLanguageCode() {
        return language.getCode();
    }

    public Pattern getWordPattern() {
        return wordPattern;
    }

    public Pattern getSentencePattern() {
        return sentencePattern;
    }

    public double getMinLanguageRatio() {
        return minLanguageRatio;
    }

    public boolean isTargetLanguageSentence(String sentence) {
        if (sentence.isEmpty()) return false;
        if (minLanguageRatio <= 0) return true;
        
        int languageChars = 0;
        java.util.regex.Matcher matcher = wordPattern.matcher(sentence);
        while (matcher.find()) {
            languageChars += matcher.group().length();
        }

        return (double) languageChars / sentence.length() > minLanguageRatio;
    }
}
