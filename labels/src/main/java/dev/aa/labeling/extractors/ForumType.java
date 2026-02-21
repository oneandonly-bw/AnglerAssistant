package dev.aa.labeling.extractors;

public enum ForumType {
    PHPBB("phpbb"),
    VBULLETIN("vbulletin");

    private final String value;

    ForumType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ForumType fromValue(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Forum type cannot be null");
        }
        for (ForumType type : ForumType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported forum type: " + value);
    }
}
