package dev.aa.labeling.labeler;

public class MaxSentencesReachedException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    private final int maxSentences;
    
    public MaxSentencesReachedException(int maxSentences) {
        super("MAX_SENTENCES_REACHED: " + maxSentences);
        this.maxSentences = maxSentences;
    }
    
    public int getMaxSentences() {
        return maxSentences;
    }
}
