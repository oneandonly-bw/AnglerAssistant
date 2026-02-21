package dev.aa.labeling.config;

import java.util.List;

public record MetaConfiguration(
    List<String> forumTypes,
    List<String> filterTypes,
    List<String> languages
) {}
