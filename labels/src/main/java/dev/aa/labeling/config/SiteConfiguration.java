package dev.aa.labeling.config;

public record SiteConfiguration(
    String siteId,
    String name,
    String baseUrl,
    int httpTimeout,
    String httpUserAgent
) {}
