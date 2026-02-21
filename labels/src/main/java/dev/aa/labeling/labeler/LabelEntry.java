package dev.aa.labeling.labeler;

public record LabelEntry(String surface, String canonical, String variant, int start, int end) {
    public LabelEntry {
        if (surface == null) throw new IllegalArgumentException("surface cannot be null");
        if (canonical == null) throw new IllegalArgumentException("canonical cannot be null");
    }
}
