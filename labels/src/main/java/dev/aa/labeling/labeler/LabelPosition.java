package dev.aa.labeling.labeler;

public record LabelPosition(String surface, String canonical, String variant, int start, int end) {
    public LabelPosition {
        if (start < 0) throw new IllegalArgumentException("start cannot be negative");
        if (end < start) throw new IllegalArgumentException("end cannot be less than start");
    }
}
