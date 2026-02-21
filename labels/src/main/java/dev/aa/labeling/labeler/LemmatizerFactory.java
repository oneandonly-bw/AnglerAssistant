package dev.aa.labeling.labeler;

import java.util.Locale;

public class LemmatizerFactory {
    
    public static Lemmatizer createLemmatizer(String language) {
        if (language == null) {
            return new NoOpLemmatizer();
        }
        
        String lang = language.toLowerCase(Locale.ROOT);
        
        return switch (lang) {
            case "ru", "ru-ru" -> new RussianLemmatizer();
            case "en", "en-us" -> new EnglishLemmatizer();
            case "he", "he-il" -> new HebrewLemmatizer();
            default -> new NoOpLemmatizer();
        };
    }
}
