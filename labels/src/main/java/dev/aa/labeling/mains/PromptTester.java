package dev.aa.labeling.mains;

import dev.aa.labeling.Constants;
import dev.aa.labeling.llm.LLMProviderManager;
import dev.aa.labeling.llm.LLMResponse;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class PromptTester {
    
    public static void main(String[] args) throws Exception {
        String configDir = Constants.DEFAULT_LLM_CONFIG_PATH;
        
        String systemPromptTemplate = """
            You are a Russian linguist.  

            Answer TRUE if Candidate is exactly Base, a valid Russian grammatical inflection of Base (any case, singular/plural), or a known diminutive/variant of Base.  

            Answer FALSE if Candidate is unrelated, derived from another word, or does not come from Base.  

            Do not guess, do not use semantic or substring similarity. Output TRUE or FALSE only.
            Base: {Base}
            Candidate: {Candidate}
            """;
        
        LLMProviderManager manager = new LLMProviderManager(java.nio.file.Path.of(configDir));
        
        if (!manager.hasProviders()) {
            System.err.println("No LLM providers available");
            System.exit(1);
        }
        
        // Dictionary base forms (canonical keys)
        String[] baseForms = {"карп", "сазан", "сом", "мушт", "толстолобик", "усач", "тилапия", "белый амур"};
        
        // Read terms from file (surface forms)
        List<String> surfaceForms = Files.readAllLines(Paths.get("data/israfish/terms_seen.txt"));
        surfaceForms.removeIf(String::isEmpty);
        
        // Form 50 random pairs
        Random rand = new Random();
        System.out.println("Testing 50 random pairs:");
        System.out.println("---");
        
        int tests = 0;
        int trueCount = 0;
        int falseCount = 0;
        
        while (tests < 50) {
            String base = baseForms[rand.nextInt(baseForms.length)];
            String candidate = surfaceForms.get(rand.nextInt(surfaceForms.size()));
            
            String systemPrompt = systemPromptTemplate.replace("{Base}", base).replace("{Candidate}", candidate);
            
            LLMResponse response = manager.chat(systemPrompt, "");
            String result = response.getContent().trim().toUpperCase();
            
            boolean isTrue = result.startsWith("TRUE");
            if (isTrue) trueCount++;
            else falseCount++;
            
            System.out.println("Base: " + base + " | Candidate: " + candidate + " → " + (isTrue ? "TRUE" : "FALSE"));
            
            tests++;
            
            // Small delay to avoid rate limiting
            Thread.sleep(500);
        }
        
        manager.close();
        System.out.println("---");
        System.out.println("Results: TRUE=" + trueCount + ", FALSE=" + falseCount);
        System.out.println("Done!");
    }
}
