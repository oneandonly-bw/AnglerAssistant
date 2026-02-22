package dev.aa.labeling.config;

import com.fasterxml.jackson.annotation.JsonProperty;
import dev.aa.labeling.Constants;
import java.nio.file.Path;
import java.util.List;

public record LabelerConfiguration(
    @JsonProperty(value = "enabled", defaultValue = "true") boolean enabled,
    @JsonProperty(value = "minSentenceLength", defaultValue = "15") int minSentenceLength,
    @JsonProperty(value = "maxSentenceLengthForContext", defaultValue = "200") int maxSentenceLengthForContext,
    @JsonProperty(value = "minLanguageRatio", defaultValue = "0.3") double minLanguageRatio,
    @JsonProperty(value = "maxSpecialCharRatio", defaultValue = "0.2") double maxSpecialCharRatio,
    @JsonProperty(value = "dictionaryPaths") List<String> dictionaryPaths,
    @JsonProperty(value = "dataDirectory") Path dataDirectory,
    @JsonProperty(value = "outputDirectory") Path outputDirectory,
    @JsonProperty(value = "outputFileName") String outputFileName,
    @JsonProperty(value = "language") String language,
    @JsonProperty(value = "forumName") String forumName,
    @JsonProperty(value = "maxSentences", defaultValue = "0") int maxSentences
) {
    public static LabelerConfiguration defaults() {
        return new LabelerConfiguration(
            true,
            Constants.DEFAULT_MIN_SENTENCE_LENGTH,
            Constants.DEFAULT_MAX_SENTENCE_LENGTH_FOR_CONTEXT,
            Constants.DEFAULT_MIN_LANGUAGE_RATIO,
            Constants.DEFAULT_MAX_SPECIAL_CHAR_RATIO,
            List.of(Constants.DEFAULT_DICTIONARY_PATH),
            Path.of(Constants.DATA_ROOT),
            Path.of(Constants.OUTPUT_DIR, Constants.LABELED_DIR),
            "labels.json",
            null,
            null,
            0
        );
    }
}
