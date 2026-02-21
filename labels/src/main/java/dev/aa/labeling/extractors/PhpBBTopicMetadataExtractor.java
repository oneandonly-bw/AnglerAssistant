package dev.aa.labeling.extractors;

import dev.aa.labeling.model.Topic;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PhpBBTopicMetadataExtractor implements TopicMetadataExtractor {
    
    private static final Pattern AUTHOR_DATE_PATTERN = Pattern.compile(
        "(\\w+)\\s*&raquo;\\s*(\\d{1,2}-\\d{1,2}-\\d{4}\\s+\\d{1,2}:\\d{2})",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");
    
    @Override
    public void extractMetadata(Topic topic, String htmlContent) {
        if (htmlContent == null || htmlContent.isEmpty()) {
            return;
        }
        
        Document doc = Jsoup.parse(htmlContent);
        
        Elements posts = doc.select("div.postbody");
        
        if (!posts.isEmpty()) {
            Element firstPost = posts.first();
            
            if (firstPost != null) {
                Element authorElem = firstPost.selectFirst("strong");
                if (authorElem != null) {
                    topic.setAuthor(authorElem.text().trim());
                }
                
                String postContent = firstPost.text();
                Matcher matcher = AUTHOR_DATE_PATTERN.matcher(postContent);
                if (matcher.find()) {
                    try {
                        String dateStr = matcher.group(2);
                        LocalDateTime dateTime = LocalDateTime.parse(dateStr, DATE_FORMATTER);
                        topic.setCreationDate(dateTime);
                    } catch (Exception e) {
                        System.err.println("Failed to parse date: " + e.getMessage());
                    }
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
            String title = titleElem.text();
            int separatorIndex = title.lastIndexOf(" - ");
            if (separatorIndex > 0) {
                return title.substring(0, separatorIndex).trim();
            }
            return title.trim();
        }
        
        return null;
    }
}
