package dev.aa.labeling.config;

import java.util.List;

public record ForumConfiguration(
    String url,
    String forumName,
    String path,
    boolean enabled,
    String forumType,
    String language,
    List<FilterConfig> include,
    List<FilterConfig> exclude
) {}
