package dev.aa.labeling.extractors;

import dev.aa.labeling.model.Topic;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VBulletinTopicMetadataExtractor implements TopicMetadataExtractor {
    
    private static final Pattern DATE_PATTERN = Pattern.compile(
        "datetime=\"([^\"]+)\"",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DATE_PATTERN2 = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2}[T\\s]\\d{2}:\\d{2}:\\d{2})",
        Pattern.CASE_INSENSITIVE
    );
    
    @Override
    public void extractMetadata(Topic topic, String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return;
        }
        
        Document doc = Jsoup.parse(htmlContent);
        
        Elements usernameLinks = doc.select("a.username");
        if (!usernameLinks.isEmpty()) {
            Element authorElem = usernameLinks.first();
            if (authorElem != null) {
                topic.setAuthor(authorElem.text().trim());
            }
        }
        
        Matcher dateMatcher = DATE_PATTERN.matcher(htmlContent);
        if (dateMatcher.find()) {
            try {
                String dateStr = dateMatcher.group(1).replace(" ", "T");
                topic.setCreationDate(LocalDateTime.parse(dateStr));
            } catch (Exception e) {
                System.err.println("Failed to parse date: " + e.getMessage());
            }
        }
        
        if (topic.getCreationDate() == null) {
            Matcher dateMatcher2 = DATE_PATTERN2.matcher(htmlContent);
            if (dateMatcher2.find()) {
                try {
                    String dateStr = dateMatcher2.group(1).replace(" ", "T");
                    topic.setCreationDate(LocalDateTime.parse(dateStr));
                } catch (Exception e) {
                    System.err.println("Failed to parse date: " + e.getMessage());
                }
            }
        }
    }
    
    @Override
    public String extractTitle(String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return null;
        }
        
        Document doc = Jsoup.parse(htmlContent);
        Element titleElem = doc.selectFirst("title");
        
        if (titleElem != null) {
            return titleElem.text().trim();
        }
        
        return null;
    }
}
