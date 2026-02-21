package dev.aa.labeling.config;

import java.util.List;

public record Configuration(
    MetaConfiguration meta,
    GeneralConfiguration general,
    SiteConfiguration site,
    RuntimeConfiguration runtime,
    LabelerConfiguration labeler,
    List<ForumConfiguration> forums
) {}
