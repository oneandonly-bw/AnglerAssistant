package dev.aa.labeling.mains;

import dev.aa.labeling.Constants;
import dev.aa.labeling.config.Configuration;
import dev.aa.labeling.config.ConfigurationFacade;
import dev.aa.labeling.config.ForumConfiguration;
import dev.aa.labeling.config.LabelerConfiguration;
import dev.aa.labeling.factory.DownloaderFactory;
import dev.aa.labeling.interfaces.IfDownloader;
import dev.aa.labeling.labeler.SentencesLabeler;
import dev.aa.labeling.labeler.OutputWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class IsrafishMain {
    private static volatile SentencesLabeler currentLabeler;
    
    public static void main(String[] args) {
        String configPath = "config/israfish_config.json";
        Path llmConfigDir = Path.of(Constants.DEFAULT_LLM_CONFIG_PATH);
        
        try {
            Configuration config = ConfigurationFacade.getConfiguration(configPath);
            
            System.out.println("=== IsraFish Labeler ===");
            System.out.println("Config: " + configPath);
            
            if (config.forums() == null || config.forums().isEmpty()) {
                System.err.println("No forums configured");
                System.exit(1);
            }
            
            for (ForumConfiguration forum : config.forums()) {
                if (forum == null) {
                    continue;
                }
                
                if (!forum.enabled()) {
                    System.out.println("Skipping disabled forum: " + forum.forumName());
                    continue;
                }
                
                processForum(config, forum, llmConfigDir);
            }
            
            System.out.println("\nAll forums processed!");
            
        } catch (ConfigurationFacade.ConfigurationException e) {
            System.err.println("Configuration error: " + e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null) {
                System.err.println("Caused by: " + cause.getMessage());
            }
            System.exit(1);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid configuration: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("IO error: " + e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null) {
                System.err.println("Caused by: " + cause.getMessage());
            }
            System.exit(1);
        }
    }
    
    private static void processForum(Configuration config, ForumConfiguration forum, Path llmConfigDir) throws Exception {
        System.out.println("\n==================================================");
        System.out.println("Processing forum: " + forum.forumName());
        System.out.println("==================================================");
        
        LabelerConfiguration baseConfig = config.labeler();
        if (baseConfig == null) {
            throw new IllegalArgumentException("Labeler configuration is null");
        }
        
        Path outputDirectory = baseConfig.outputDirectory() != null 
            ? baseConfig.outputDirectory() 
            : Path.of("output");
        
        String siteId = config.site() != null ? config.site().siteId() : "default";
        String outputFileName = generateOutputFileName(baseConfig.dictionaryPaths(), siteId);
        
        Path outputFile = outputDirectory.resolve(outputFileName);
        
        Path dataDirectory = baseConfig.dataDirectory() != null 
            ? baseConfig.dataDirectory() 
            : Path.of(Constants.DATA_ROOT);
        
        LabelerConfiguration labelerConfig = createLabelerConfiguration(
            baseConfig, outputDirectory, outputFileName, forum.language(), siteId, dataDirectory, siteId);
        
        OutputWriter writer = new OutputWriter(outputDirectory, outputFileName);
        SentencesLabeler labeler = new SentencesLabeler(labelerConfig, writer, llmConfigDir);
        currentLabeler = labeler;
        
        Configuration forumConfig = new Configuration(
            config.meta(),
            config.general(),
            config.site(),
            config.runtime(),
            labelerConfig,
            List.of(forum)
        );
        
        IfDownloader downloader = DownloaderFactory.create(forumConfig, labeler);
        
        try {
            downloader.download();
        } finally {
            if (labeler != null) {
                labeler.close();
            }
        }
    }
    
    private static LabelerConfiguration createLabelerConfiguration(
            LabelerConfiguration base,
            Path outputDirectory,
            String outputFileName,
            String language,
            String forumName,
            Path dataDirectory,
            String siteId) {
        
        return new LabelerConfiguration(
            base.enabled(),
            base.minSentenceLength(),
            base.maxSentenceLengthForContext(),
            base.minLanguageRatio(),
            base.maxSpecialCharRatio(),
            base.dictionaryPaths(),
            dataDirectory,
            outputDirectory,
            outputFileName,
            language,
            forumName,
            base.maxSentences(),
            siteId
        );
    }
    
    private static String generateOutputFileName(List<String> dictionaryPaths, String siteId) {
        String sitePrefix = (siteId != null && !siteId.isEmpty()) ? siteId + "_" : "";
        
        if (dictionaryPaths == null || dictionaryPaths.isEmpty()) {
            return sitePrefix + "output_labeled.json";
        }
        
        String dictPath = dictionaryPaths.get(0);
        String fileName = Paths.get(dictPath).getFileName().toString();
        
        if (fileName.endsWith("_dict.json")) {
            fileName = fileName.replace("_dict.json", ".json");
        } else if (fileName.endsWith(".json")) {
            fileName = fileName.replace(".json", ".json");
        } else {
            fileName = fileName + ".json";
        }
        
        return sitePrefix + fileName;
    }
}
