package dev.aa.labeling.labeler;

import java.util.List;

public record LabelingResult(List<LabeledSentence> sentences, LabelingMetadata metadata) {
    
    public int getTotalSentences() {
        return sentences.size();
    }
}
