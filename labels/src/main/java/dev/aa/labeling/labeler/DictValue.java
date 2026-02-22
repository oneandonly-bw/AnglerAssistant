package dev.aa.labeling.labeler;

public record DictValue(String value, String specificity, Duality duality) {
    public DictValue(String value, String specificity) {
        this(value, specificity, null);
    }
    
    public DictValue {
        if (value == null || value.isEmpty()) {
            throw new IllegalArgumentException("DictValue value cannot be null or empty");
        }
    }
}
