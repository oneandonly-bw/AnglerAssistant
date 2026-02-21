package dev.aa.labeling.mains;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetGroqLlmList {
    
    public static void main(String[] args) throws Exception {
        String apiKey = "YOUR_API_KEY_HERE";
        String url = "https://api.groq.com/openai/v1/models";
        
        HttpClient client = HttpClient.newHttpClient();
        
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + apiKey)
            .header("Content-Type", "application/json")
            .GET()
            .build();
        
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        
        String json = response.body();
        
        System.out.println("Available Groq Models:");
        System.out.println("======================");
        System.out.printf("%-50s %10s %12s%n", "Model ID", "Context", "Max Tokens");
        System.out.println("---------------------------------------------------------------------");
        
        Pattern p = Pattern.compile("\"id\":\"([^\"]+)\".*?\"context_window\":(\\d+).*?\"max_completion_tokens\":(\\d+)");
        Matcher m = p.matcher(json);
        
        while (m.find()) {
            String id = m.group(1);
            int context = Integer.parseInt(m.group(2));
            int maxTokens = Integer.parseInt(m.group(3));
            System.out.printf("%-50s %10d %12d%n", id, context, maxTokens);
        }
    }
}
