package dev.aa.labeling.labeler;

public class LabelingMetadata {
    private final String language;
    private final String extractionDate;
    private final int totalSentences;
    private final int totalLabelsLoaded;
    private final int totalTopicsProcessed;

    public LabelingMetadata(String language, String extractionDate, int totalSentences, 
                           int totalLabelsLoaded, int totalTopicsProcessed) {
        this.language = language;
        this.extractionDate = extractionDate;
        this.totalSentences = totalSentences;
        this.totalLabelsLoaded = totalLabelsLoaded;
        this.totalTopicsProcessed = totalTopicsProcessed;
    }

    public String getLanguage() {
        return language;
    }

    public String getExtractionDate() {
        return extractionDate;
    }

    public int getTotalSentences() {
        return totalSentences;
    }

    public int getTotalLabelsLoaded() {
        return totalLabelsLoaded;
    }

    public int getTotalTopicsProcessed() {
        return totalTopicsProcessed;
    }
}
