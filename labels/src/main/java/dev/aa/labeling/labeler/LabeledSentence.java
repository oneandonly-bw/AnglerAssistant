package dev.aa.labeling.labeler;

import java.util.List;

public record LabeledSentence(
    String forumUrl, 
    String topicUrl, 
    String lang, 
    String text,
    List<LabelPosition> labels
) {}
