package dev.aa.labeling.labeler;

public class NoOpLemmatizer implements Lemmatizer {
    @Override
    public String lemmatize(String word) {
        return word;
    }
}
