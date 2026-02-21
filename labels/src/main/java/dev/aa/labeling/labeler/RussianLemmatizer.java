package dev.aa.labeling.labeler;

import dev.aa.labeling.Constants;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class RussianLemmatizer implements Lemmatizer {
    
    private final HttpClient httpClient;
    
    public RussianLemmatizer() {
        this.httpClient = HttpClient.newHttpClient();
    }
    
    @Override
    public String lemmatize(String word) {
        try {
            String encodedWord = URLEncoder.encode(word, StandardCharsets.UTF_8);
            String url = Constants.LEMMA_SERVICE_URL + "?word=" + encodedWord;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(java.time.Duration.ofSeconds(5))
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                return response.body();
            } else {
                System.err.println("Lemma service returned status: " + response.statusCode());
                System.exit(1);
                return null;
            }
        } catch (java.net.http.HttpTimeoutException e) {
            System.err.println("Lemma service timeout");
            System.exit(1);
            return null;
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg == null) {
                msg = e.getClass().getName();
            }
            System.err.println("Lemma service error: " + msg);
            System.exit(1);
            return null;
        }
    }
}
