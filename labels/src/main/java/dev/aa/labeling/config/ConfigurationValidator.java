package dev.aa.labeling.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConfigurationValidator {

    private final JsonSchema schema;
    private final ObjectMapper objectMapper;

    public ConfigurationValidator() {
        try (InputStream schemaStream = getClass().getResourceAsStream("/config/config_schema.json")) {
            JsonSchemaFactory schemaFactory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
            this.schema = schemaFactory.getSchema(schemaStream);
            this.objectMapper = new ObjectMapper();
        } catch (Exception e) {
            throw new RuntimeException("Failed to load JSON schema", e);
        }
    }

    public ValidationResult validate(String jsonContent) {
        try {
            JsonNode jsonNode = objectMapper.readTree(jsonContent);
            Set<ValidationMessage> errors = schema.validate(jsonNode);

            if (!errors.isEmpty()) {
                return new ValidationResult(false, errors.stream()
                        .map(ValidationMessage::getMessage)
                        .collect(Collectors.toList()));
            }

            List<String> customErrors = validateCustomRules(jsonNode);
            if (!customErrors.isEmpty()) {
                return new ValidationResult(false, customErrors);
            }

            return new ValidationResult(true, List.of());
        } catch (Exception e) {
            return new ValidationResult(false, List.of("Failed to parse JSON: " + e.getMessage()));
        }
    }

    private List<String> validateCustomRules(JsonNode root) {
        Set<String> errors = new java.util.HashSet<>();

        JsonNode forums = root.get("forums");
        if (forums != null && forums.isArray()) {
            for (int i = 0; i < forums.size(); i++) {
                JsonNode forum = forums.get(i);
                boolean hasInclude = forum.has("include") && !forum.get("include").isNull();
                boolean hasExclude = forum.has("exclude") && !forum.get("exclude").isNull();

                if (hasInclude && hasExclude) {
                    String forumName = forum.has("forumName") ? forum.get("forumName").asText() : "forum at index " + i;
                    errors.add("Forum '" + forumName + "': cannot have both 'include' and 'exclude' - use only one");
                }
            }
        }

        return new ArrayList<>(errors);
    }

    public record ValidationResult(boolean isValid, List<String> errors) {}
}
