package dev.aa.labeling.labeler;

public record Candidate(
    String surface,
    int start,
    int end,
    String canonical,
    String entryType,
    DictValue dictValue
) {}
