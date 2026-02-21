package dev.aa.labeling.extractors;

import dev.aa.labeling.Constants;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class TopicsListExtractor {
    
    private static final String DEFAULT_BASE_URL = "https://forum.israfish.co.il";
    private String baseUrl = DEFAULT_BASE_URL;
    
    public TopicsListExtractor() {}
    
    public TopicsListExtractor(String baseUrl) {
        this.baseUrl = baseUrl;
    }
    
    public List<String> getTopicsList(ForumType forumType, String forumUrl) {
        return switch (forumType) {
            case PHPBB -> getPhpBBForumTopicsList(forumUrl);
            case VBULLETIN -> getVBulletinForumTopicsList(forumUrl);
        };
    }
    
    protected List<String> getPhpBBForumTopicsList(String forumUrl) {
        List<String> topicUrls = new ArrayList<>();
        
        int page = 0;
        int maxPages = 10;
        
        while (page < maxPages) {
            String url = forumUrl + (page == 0 ? "" : "&start=" + (page * 25));
            
            try {
                String html = getForumHtml(url);
                Document document = parseHtml(html);
                
                Elements links = document.select("a.topictitle");
                
                if (links.isEmpty()) {
                    break;
                }
                
                for (Element link : links) {
                    String href = link.attr("href");
                    
                    if (href.isEmpty()) {
                        continue;
                    }
                    
                    String absoluteUrl = makeAbsolute(baseUrl, href);
                    absoluteUrl = absoluteUrl.replaceAll("&start=\\d+", "");
                    absoluteUrl = absoluteUrl.replaceAll("&sid=[^&]+", "");
                    absoluteUrl = absoluteUrl.replaceAll("#.*$", "");
                    
                    if (!topicUrls.contains(absoluteUrl)) {
                        topicUrls.add(absoluteUrl);
                    }
                }
                
                Elements nextPage = document.select("a[rel='next']");
                if (nextPage.isEmpty()) {
                    break;
                }
                
                page++;
                
            } catch (Exception e) {
                System.err.println("Error extracting topics from page " + page + ": " + e.getMessage());
                break;
            }
        }
        
        topicUrls = removeDuplicates(topicUrls);
        return topicUrls;
    }
    
    protected List<String> getVBulletinForumTopicsList(String forumUrl) {
        List<String> topicUrls = new ArrayList<>();
        
        try {
            String html = getForumHtml(forumUrl);
            Document document = parseHtml(html);
            
            Elements links = document.select("a");
            
            for (Element link : links) {
                String href = link.attr("href");
                
                if (href.isEmpty()) {
                    continue;
                }
                
                // Detect classic pattern: showthread.php?t=
                if (href.contains("showthread.php?t=")) {
                    String absoluteUrl = makeAbsolute(baseUrl, href);
                    topicUrls.add(absoluteUrl);
                    continue;
                }
                
                // Detect SEO pattern: /threads/.*\.[0-9]+/?$
                if (href.matches(".*/threads/.*\\.\\d+/?.*")) {
                    String absoluteUrl = makeAbsolute(baseUrl, href);
                    topicUrls.add(absoluteUrl);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Error extracting topics from " + forumUrl + ": " + e.getMessage());
        }
        
        topicUrls = removeDuplicates(topicUrls);
        return topicUrls;
    }
    
    protected String getForumHtml(String url) throws Exception {
        return Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            .timeout(Constants.DEFAULT_HTTP_TIMEOUT_MS)
            .get()
            .html();
    }
    
    protected Document parseHtml(String html) {
        return Jsoup.parse(html);
    }
    
    protected String makeAbsolute(String baseUrl, String href) {
        if (href.startsWith("http")) {
            return href;
        }
        
        if (href.startsWith("./")) {
            href = href.substring(2);
        } else if (href.startsWith("/")) {
            href = href.substring(1);
        }
        
        String base = baseUrl;
        if (!base.endsWith("/")) {
            base += "/";
        }
        
        return base + href;
    }
    
    protected List<String> removeDuplicates(List<String> urls) {
        return new ArrayList<>(new HashSet<>(urls));
    }
}
