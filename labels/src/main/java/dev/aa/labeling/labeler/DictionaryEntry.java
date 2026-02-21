package dev.aa.labeling.labeler;

import java.util.List;

public record DictionaryEntry(
    String uid,
    String type,
    List<DictValue> values
) {
    public String getCanonical() {
        if (values == null) return null;
        for (DictValue v : values) {
            if ("CANONICAL".equals(v.specificity())) {
                return v.value();
            }
        }
        return null;
    }
}
