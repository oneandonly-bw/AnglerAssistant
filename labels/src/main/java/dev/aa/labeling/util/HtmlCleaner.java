package dev.aa.labeling.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.Set;
import java.util.regex.Pattern;

public class HtmlCleaner {
    
    private static final Set<String> SCRIPT_TAGS = Set.of(
        "script", "noscript", "iframe", "embed", "object"
    );
    
    private static final Set<String> MEDIA_TAGS = Set.of(
        "img", "picture", "source", "video", "audio", "svg", "use"
    );
    
    private static final Set<String> NAVIGATION_TAGS = Set.of(
        "nav", "menu", "header", "footer", "aside", "div"
    );
    
    private static final Set<String> NAVIGATION_IDS = Set.of(
        "social", "breadcrumb", "navbar", "pagination", "jumpbox", "action-bar", "back2top", "signature"
    );
    
    private static final Set<String> FORM_TAGS = Set.of(
        "form", "input", "button", "select", "textarea", "option", "label"
    );
    
    private static final Set<String> STYLING_TAGS = Set.of(
        "style", "link"
    );
    
    private static final Set<String> ADVERTISEMENT_CLASSES = Set.of(
        "ad", "advertisement", "banner", "sidebar", "sponsor", "promo"
    );
    
    private static final Set<String> ANNOTATION_CLASSES = Set.of(
        "annotation", "note", "highlight", "signature", "mod-note", "edited", 
        "quote-author", "mark", "ins", "del", "s"
    );
    
    private static final Set<String> QUOTE_CLASSES = Set.of(
        "blockquote", "quote"
    );
    
    private static final Set<String> NAVIGATION_CLASSES = Set.of(
        "navbar", "breadcrumb", "pagination", "navlinks"
    );
    
    private static final Pattern BBCODE_PATTERN = Pattern.compile(
        "\\[/?\\w+\\]|\\[img\\].*?\\[/img\\]|\\[url\\].*?\\[/url\\]|\\[table\\].*?\\[/table\\]|\\[td\\].*?\\[/td\\]|\\[tr\\].*?\\[/tr\\]|\\[b\\].*?\\[/b\\]|\\[i\\].*?\\[/i\\]|\\[u\\].*?\\[/u\\]",
        Pattern.DOTALL
    );
    
    private static final Pattern ARROW_SYMBOLS = Pattern.compile("[⇧⇩↧↨↩⇒⇐↔⇑⇓]");
    
    public static String cleanHtml(String htmlContent) {
        return cleanHtml(htmlContent, null);
    }
    
    public static String cleanHtml(String htmlContent, String topicUrl) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return "";
        }
        
        try {
            Document doc = Jsoup.parse(htmlContent);
            
            Elements postContents = extractPostContent(doc);
            
            if (postContents.isEmpty()) {
                return "";
            }
            
            for (Element post : postContents) {
                removeNavigationElements(post);
                removeQuoteBlocks(post);
                removeImageGalleries(post);
                removeForms(post);
            }
            
            StringBuilder content = new StringBuilder();
            
            if (topicUrl != null) {
                content.append("Topic URL: ").append(topicUrl).append("\n");
                content.append("Downloaded: ").append(java.time.LocalDateTime.now()).append("\n");
                content.append("=====================================\n\n");
            }
            
            for (Element post : postContents) {
                String postText = post.text().trim();
                
                postText = removeBBCode(postText);
                postText = removeArrowSymbols(postText);
                postText = cleanWhitespace(postText);
                
                if (postText.length() < 50) {
                    continue;
                }
                
                if (isLikelyNavigation(postText)) {
                    continue;
                }
                
                content.append(postText).append("\n\n");
            }
            
            return content.toString().trim();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to clean HTML content", e);
        }
    }
    
    private static Elements extractPostContent(Document doc) {
        Elements postContents = doc.select("div.postbody > div.content");
        if (!postContents.isEmpty()) return postContents;
        
        postContents = doc.select("div.postbody div.content");
        if (!postContents.isEmpty()) return postContents;
        
        postContents = doc.select("div.content");
        if (!postContents.isEmpty()) return postContents;
        
        postContents = doc.select(".postbody .content");
        if (!postContents.isEmpty()) return postContents;
        
        postContents = doc.select("div.postbody");
        if (!postContents.isEmpty()) return postContents;
        
        postContents = doc.select("div.message");
        if (!postContents.isEmpty()) return postContents;
        
        postContents = doc.select("article.post");
        if (!postContents.isEmpty()) return postContents;
        
        return new Elements();
    }
    
    private static void removeNavigationElements(Element element) {
        for (String tag : NAVIGATION_TAGS) {
            element.select(tag).remove();
        }
        
        for (String cls : NAVIGATION_CLASSES) {
            element.select("." + cls).remove();
        }
        
        for (String id : NAVIGATION_IDS) {
            element.select("#" + id).remove();
        }
        
        element.select("a[href*='viewforum.php']").remove();
        element.select("a[href*='index.php']").remove();
        element.select("a[href*='viewtopic.php']").remove();
    }
    
    private static void removeQuoteBlocks(Element element) {
        for (String cls : QUOTE_CLASSES) {
            element.select(cls).remove();
        }
        element.select(".signature").remove();
        element.select(".notice").remove();
    }
    
    private static void removeImageGalleries(Element element) {
        element.select("table:contains([img]), table:contains(PhotoAlbums)").remove();
        element.select("a[href*='PhotoAlbums']").remove();
        element.select("a[href*='.jpg']").remove();
        element.select("a[href*='.png']").remove();
        element.select("a[href*='thumbnails']").remove();
        element.select("img").remove();
    }
    
    private static void removeForms(Element element) {
        for (String tag : FORM_TAGS) {
            element.select(tag).remove();
        }
    }
    
    private static String removeBBCode(String text) {
        return BBCODE_PATTERN.matcher(text).replaceAll("");
    }
    
    private static String removeArrowSymbols(String text) {
        return ARROW_SYMBOLS.matcher(text).replaceAll("");
    }
    
    private static boolean isLikelyNavigation(String text) {
        if (text.length() > 2000 && (text.contains("↳") || text.contains("Перейти"))) {
            return true;
        }
        
        if (text.contains("↳")) {
            return true;
        }
        
        if (text.contains("[img]") || text.contains("[url]") || 
            text.contains("PhotoAlbums") || text.contains("thumbnails") ||
            text.contains("[table]") || text.contains("↧")) {
            return text.length() > 500;
        }
        
        return false;
    }
    
    private static String cleanWhitespace(String text) {
        text = text.replaceAll("[ \t]+", " ");
        text = text.replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }
}
