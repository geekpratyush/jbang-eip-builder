package beans;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TemplateEngine {
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{type:([^}]+)\\}\\}");
    
    // Holds the generator and its expected arity (parameter count)
    private static class GeneratorDef {
        final int arity;
        final DataGenerator generator;

        GeneratorDef(int arity, DataGenerator generator) {
            this.arity = arity;
            this.generator = generator;
        }
    }

    private final Map<String, GeneratorDef> generators = new HashMap<>();

    public void registerGenerator(String name, int arity, DataGenerator generator) {
        generators.put(name, new GeneratorDef(arity, generator));
    }

    public String process(String template, GenerationContext context, DatabaseService dbService) {
        if (template == null) return "";
        
        try {
            StringBuilder sb = new StringBuilder();
            Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
            int lastEnd = 0;
            
            while (matcher.find()) {
                sb.append(template, lastEnd, matcher.start());
                String placeholder = matcher.group(1);
                sb.append(resolvePlaceholder(placeholder, context, dbService));
                lastEnd = matcher.end();
            }
            sb.append(template.substring(lastEnd));
            return sb.toString();
        } catch (Exception e) {
            return "Faker Error: " + e.getMessage();
        }
    }

    private String resolvePlaceholder(String placeholder, GenerationContext context, DatabaseService dbService) {
        String[] parts = placeholder.split(":");
        String generatorOrDatasetKey = parts[0];

        // Determine if this is a registered computational generator
        GeneratorDef def = generators.get(generatorOrDatasetKey);
        
        String role = null;
        String[] params;

        if (def != null) {
            // Computational generator
            int arity = def.arity;
            int providedParamsCount = parts.length - 1;
            
            if (providedParamsCount > arity) {
                role = parts[parts.length - 1];
                params = Arrays.copyOfRange(parts, 1, 1 + arity);
            } else {
                params = Arrays.copyOfRange(parts, 1, parts.length);
            }

            // Resolve parameters (cross-referencing)
            String[] resolvedParams = new String[params.length];
            for (int i = 0; i < params.length; i++) {
                resolvedParams[i] = resolveParam(params[i], context, dbService, role);
            }

            // Check caching
            if (role != null) {
                String cacheKey = generatorOrDatasetKey + ":" + role;
                String cachedVal = context.getCachedValue(cacheKey);
                if (cachedVal != null) {
                    return cachedVal;
                }
                String val = def.generator.generate(resolvedParams);
                context.cacheValue(cacheKey, val);
                return val;
            } else {
                return def.generator.generate(resolvedParams);
            }
        } else {
            // Database-backed dataset / property lookup
            String datasetName = generatorOrDatasetKey;
            String propertyName = "";

            if (generatorOrDatasetKey.contains(".")) {
                String[] dotParts = generatorOrDatasetKey.split("\\.");
                datasetName = dotParts[0];
                propertyName = dotParts[1];
            }

            if (parts.length > 1) {
                role = parts[parts.length - 1];
            }

            String cacheKey = generatorOrDatasetKey + ":" + (role != null ? role : "default");
            String cachedVal = context.getCachedValue(cacheKey);
            if (cachedVal != null) {
                return cachedVal;
            }

            String val = dbService.getRandomValue(datasetName, propertyName, context, role);
            if (val == null) {
                // If not found in DB, return as is
                return "{{" + placeholder + "}}";
            }

            if (role != null) {
                context.cacheValue(cacheKey, val);
            }
            return val;
        }
    }

    private String resolveParam(String param, GenerationContext context, DatabaseService dbService, String currentRole) {
        if (param.contains(".")) {
            String[] dotParts = param.split("\\.");
            String dataset = dotParts[0];
            String prop = dotParts[1];
            String val = dbService.getRandomValue(dataset, prop, context, currentRole);
            return val != null ? val : param;
        }
        if (param.endsWith("Country")) {
            String role = param.substring(0, param.length() - "Country".length());
            String country = dbService.getRandomValue("banks", "country", context, role);
            if (country == null || country.isEmpty()) {
                country = dbService.getRandomValue("companies", "country", context, role);
            }
            if (country == null || country.isEmpty()) {
                country = dbService.getRandomValue("countries", "", context, null);
            }
            return country != null ? country : "US";
        }
        if (param.endsWith("Bic")) {
            String role = param.substring(0, param.length() - "Bic".length());
            String bic = dbService.getRandomValue("banks", "bic", context, role);
            return bic != null ? bic : "";
        }
        return param;
    }
}
