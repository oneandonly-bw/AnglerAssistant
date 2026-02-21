package dev.aa.labeling.mains;

import dev.aa.labeling.Constants;
import dev.aa.labeling.config.*;
import dev.aa.labeling.factory.DownloaderFactory;
import dev.aa.labeling.interfaces.IfDownloader;
import dev.aa.labeling.labeler.MaxSentencesReachedException;
import dev.aa.labeling.labeler.SentencesLabeler;
import dev.aa.labeling.labeler.OutputWriter;
import dev.aa.labeling.util.CheckpointResolver;

import dev.aa.labeling.engine.BaseDownloader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;

public class LabelerMain {
    private static volatile SentencesLabeler currentLabeler;
    private static volatile Path currentOutputFile;
    
    private static void startLemmaService() {
        List<String> commands = new ArrayList<>();
        commands.add("python");
        commands.add("C:\\AnglerAsistant\\labels\\tools\\FlaskService\\lemma_service.py");
        
        Process lemmaServiceProcess = null;
        
        try {
            ProcessBuilder pb = new ProcessBuilder(commands);
            pb.redirectErrorStream(true);
            lemmaServiceProcess = pb.start();
            System.out.println("Lemma service starting...");
        } catch (Exception e) {
            System.out.println("Failed to start with 'python', trying full path...");
            List<String> fallbackCommands = new ArrayList<>();
            fallbackCommands.add("C:\\Tools\\Python311\\python.exe");
            fallbackCommands.add("C:\\AnglerAsistant\\labels\\tools\\FlaskService\\lemma_service.py");
            
            try {
                ProcessBuilder fb = new ProcessBuilder(fallbackCommands);
                fb.redirectErrorStream(true);
                lemmaServiceProcess = fb.start();
                System.out.println("Lemma service starting with full path...");
            } catch (Exception ex) {
                System.err.println("Failed to start lemma service: " + ex.getMessage());
                System.exit(1);
            }
        }
        
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public static void main(String[] args) {
        startLemmaService();
        if (args.length == 0) {
            System.out.println("Usage: java LabelerMain -config <config_path> [-resume]");
            System.out.println("  -config <path>  : Path to config file");
            System.out.println("                     - absolute path: external file (e.g., C:/config/my.json)");
            System.out.println("                     - relative path: file in resources (e.g., config/my_config.json)");
            System.out.println("  -resume         : Resume from checkpoint");
            System.out.println("");
            System.out.println("Examples:");
            System.out.println("  java LabelerMain -config config/israfish_config.json");
            System.out.println("  java LabelerMain -config C:/config/my_config.json");
            System.out.println("  java LabelerMain -config config/israfish_config.json -resume");
            System.exit(1);
        }
        
        String configPath = null;
        boolean resume = false;
        Path llmConfigDir = Path.of(Constants.DEFAULT_LLM_CONFIG_PATH);
        
        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-config" -> {
                    if (i + 1 < args.length) {
                        configPath = args[++i];
                    }
                }
                case "-resume" -> resume = true;
                default -> {
                    if (!args[i].startsWith("-")) {
                        configPath = args[i];
                    }
                }
            }
        }
        
        final boolean resumeFlag = resume;
        
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\nShutdown signal received...");
            
            if (currentOutputFile != null) {
                try {
                    System.out.println("Cleaning up incomplete topics...");
                    CheckpointResolver.cleanupIncomplete(currentOutputFile);
                    System.out.println("Cleanup complete.");
                } catch (Exception e) {
                    System.err.println("Error during cleanup: " + e.getMessage());
                }
            }
            
            if (currentLabeler != null) {
                try {
                    currentLabeler.close();
                    System.out.println("Resources saved.");
                } catch (Exception e) {
                    System.err.println("Error during shutdown: " + e.getMessage());
                }
            }
        }));
        
        try {
            System.out.println("Loading configuration: " + configPath);
            Configuration config = ConfigurationFacade.getConfiguration(configPath);
            
            if (config == null) {
                throw new IllegalArgumentException("Configuration is null");
            }
            
            SiteConfiguration site = config.site();
            if (site == null) {
                throw new IllegalArgumentException("Site configuration is null");
            }
            
            System.out.println("Site: " + site.name());
            System.out.println("Forums to process: " + config.forums().size());
            System.out.println("LLM Config: " + llmConfigDir);
            
            List<ForumConfiguration> forums = config.forums();
            if (forums.isEmpty()) {
                System.out.println("No forums configured");
                return;
            }
            
            for (ForumConfiguration forum : forums) {
                if (forum == null) {
                    System.err.println("Skipping null forum entry");
                    continue;
                }
                
                if (!forum.enabled()) {
                    System.out.println("Skipping disabled forum: " + forum.forumName());
                    continue;
                }
                
                processForum(config, forum, llmConfigDir, resumeFlag);
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
    
    private static void processForum(Configuration config, ForumConfiguration forum, Path llmConfigDir, boolean resume) throws Exception {
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
        
        currentOutputFile = outputFile;
        
        Path dataDirectory = baseConfig.dataDirectory() != null 
            ? baseConfig.dataDirectory() 
            : Path.of(Constants.DATA_ROOT);
        
        LabelerConfiguration labelerConfig = createLabelerConfiguration(
            baseConfig, outputDirectory, outputFileName, forum.language(), siteId, dataDirectory);
        
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
        
        if (downloader instanceof BaseDownloader baseDownloader) {
            var completed = CheckpointResolver.loadCompleted(outputFile);
            if (!completed.completedTopicUrls().isEmpty()) {
                if (resume) {
                    System.out.println("Will resume, skipping " + completed.completedTopicUrls().size() + " completed topics");
                    baseDownloader.setSkipTopicUrls(new HashSet<>(completed.completedTopicUrls()));
                } else {
                    System.out.println("Found " + completed.completedTopicUrls().size() + " previously completed topics (use -resume to skip)");
                }
            }
        }
        
        try {
            downloader.download();
        } catch (Exception e) {
            if (e.getCause() instanceof MaxSentencesReachedException || 
                (e.getMessage() != null && e.getMessage().contains("MAX_SENTENCES_REACHED"))) {
                System.out.println("Reached max sentences limit");
            } else {
                System.err.println("Download failed for forum " + forum.forumName() + ": " + e.getMessage());
                throw e;
            }
        } finally {
            labeler.close();
            currentLabeler = null;
            currentOutputFile = null;
        }
        
        var result = labeler.getResult();
        System.out.println("Topics processed: " + result.metadata().getTotalTopicsProcessed());
        System.out.println("Sentences labeled: " + result.metadata().getTotalSentences());
    }
    
    private static LabelerConfiguration createLabelerConfiguration(
            LabelerConfiguration baseConfig, Path outputDirectory, String outputFileName, String language, String forumName, Path dataDirectory) {
        return new LabelerConfiguration(
            baseConfig.enabled(),
            baseConfig.minSentenceLength(),
            baseConfig.maxSentenceLengthForContext(),
            baseConfig.minLanguageRatio(),
            baseConfig.maxSpecialCharRatio(),
            baseConfig.dictionaryPaths(),
            baseConfig.blockedTermsLimit(),
            dataDirectory,
            outputDirectory,
            outputFileName,
            language,
            forumName,
            baseConfig.maxSentences()
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
