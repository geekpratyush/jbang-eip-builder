package com.routebuilder.faker;

import com.routebuilder.faker.generators.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class UniversalFaker {
    private static TemplateDiscovery discovery;
    private static TemplateEngine engine;
    private static DatabaseService dbService;
    private static final GenerationContext globalContext = new GenerationContext();
    private static boolean initialized = false;

    public UniversalFaker() {
        initialize(findTemplatesPath(), findDefaultDbPath());
    }

    public UniversalFaker(Path templatesDir) {
        initialize(templatesDir, findDefaultDbPath());
    }

    public UniversalFaker(Path templatesDir, Path dbDir) {
        initialize(templatesDir, dbDir);
    }

    private void initialize(Path templatesPath, Path dbPath) {
        synchronized (UniversalFaker.class) {
            if (!initialized) {
                discovery = new TemplateDiscovery(templatesPath);
                dbService = new DatabaseService(dbPath);
                engine = new TemplateEngine();
                registerDefaultGenerators();
                discovery.scanTemplates();
                initialized = true;
            } else {
                if (templatesPath != null && !templatesPath.equals(discovery.getTemplatesDir())) {
                    discovery = new TemplateDiscovery(templatesPath);
                    discovery.scanTemplates();
                }
                if (dbPath != null && !dbPath.equals(dbService.getDbDir())) {
                    dbService = new DatabaseService(dbPath);
                }
            }
        }
    }

    private static Path findTemplatesPath() {
        String userDir = System.getProperty("user.dir");
        Path p = java.nio.file.Paths.get(userDir, "templates");
        if (java.nio.file.Files.exists(p)) return p;
        p = java.nio.file.Paths.get(userDir, "FAKER", "templates");
        if (java.nio.file.Files.exists(p)) return p;
        return java.nio.file.Paths.get(userDir, "templates");
    }

    private static Path findDefaultDbPath() {
        String userDir = System.getProperty("user.dir");
        Path p = java.nio.file.Paths.get(userDir, "faker-db");
        if (java.nio.file.Files.exists(p)) return p.resolve("financial");
        p = java.nio.file.Paths.get(userDir, "FAKER", "faker-db");
        if (java.nio.file.Files.exists(p)) return p.resolve("financial");
        return null;
    }

    public void setDatabase(Path targetDb) {
        if (java.nio.file.Files.exists(targetDb)) {
            dbService = new DatabaseService(targetDb);
            System.out.println("[UniversalFaker] Switched active database to: " + targetDb);
        } else {
            System.err.println("[UniversalFaker] Database folder not found: " + targetDb);
        }
    }

    public void setDatabase(String dbName) {
        Path baseDb = findDefaultDbPath();
        if (baseDb != null) {
            Path parentDbDir = baseDb.getParent();
            if (parentDbDir != null) {
                Path targetDb = parentDbDir.resolve(dbName);
                if (java.nio.file.Files.exists(targetDb)) {
                    setDatabase(targetDb);
                    return;
                }
            }
        }
        System.err.println("[UniversalFaker] Could not find database folder by name: " + dbName);
    }

    public void reloadDatabase() {
        if (dbService != null) {
            dbService.reloadDatabase();
        }
    }

    private void registerDefaultGenerators() {
        Random rnd = new Random();

        // 1. UUID & UETR
        engine.registerGenerator("uuid", 0, params -> UUID.randomUUID().toString());
        engine.registerGenerator("uetr", 0, params -> UUID.randomUUID().toString());

        // 2. DateTime
        engine.registerGenerator("dateTime", 1, params -> {
            String pattern = params.length > 0 ? params[0] : "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
            if ("iso".equals(pattern)) pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
            return java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern(pattern));
        });

        // 3. Amount
        engine.registerGenerator("amount", 2, params -> {
            double min = params.length > 0 ? Double.parseDouble(params[0]) : 1000.0;
            double max = params.length > 1 ? Double.parseDouble(params[1]) : 1000000.0;
            return String.format(java.util.Locale.US, "%.2f", min + rnd.nextDouble() * (max - min));
        });

        // 4. Random from inline list
        engine.registerGenerator("random", 1, params -> {
            if (params.length == 0 || params[0].isEmpty()) return "";
            String[] options = params[0].split(",");
            return options[rnd.nextInt(options.length)].trim();
        });

        // 5. IBAN
        engine.registerGenerator("iban", 1, params -> {
            String country = params.length > 0 ? params[0] : "GB";
            if (country == null || country.isEmpty() || country.length() > 3) {
                country = "GB";
            }
            int len = 14 + rnd.nextInt(8);
            return country + rnd.nextInt(10, 99) + randomAlphanumeric(rnd, len);
        });

        // 6. Technical / Codes
        engine.registerGenerator("lei", 0, params -> (randomAlphanumeric(rnd, 4) + "00" + randomAlphanumeric(rnd, 12) + rnd.nextInt(10, 99)).toUpperCase());
        engine.registerGenerator("uti", 0, params -> (randomAlphanumeric(rnd, 18) + rnd.nextInt(10, 99) + randomAlphanumeric(rnd, 10)).toUpperCase());
        engine.registerGenerator("aba", 0, params -> String.format("%09d", rnd.nextInt(10000000, 999999999)));
        engine.registerGenerator("chips", 0, params -> String.format("%04d", rnd.nextInt(1000, 9999)));
        engine.registerGenerator("sortCode", 0, params -> String.format("%02d-%02d-%02d", rnd.nextInt(10, 99), rnd.nextInt(10, 99), rnd.nextInt(10, 99)));
        engine.registerGenerator("bsb", 0, params -> String.format("%03d-%03d", rnd.nextInt(100, 999), rnd.nextInt(100, 999)));
        
        // 7. Tech and dynamic fields
        engine.registerGenerator("msgId", 0, params -> "MSG/" + randomAlphanumeric(rnd, 8).toUpperCase() + "/" + System.currentTimeMillis());
        engine.registerGenerator("uid", 1, params -> {
            int len = params.length > 0 ? Integer.parseInt(params[0]) : 16;
            return randomAlphanumeric(rnd, len).toUpperCase();
        });
        engine.registerGenerator("number", 2, params -> {
            long min = params.length > 0 ? Long.parseLong(params[0]) : 1000;
            long max = params.length > 1 ? Long.parseLong(params[1]) : 9999;
            return String.valueOf(rnd.nextLong(min, max));
        });
        engine.registerGenerator("boolean", 0, params -> String.valueOf(rnd.nextBoolean()));
        engine.registerGenerator("quarter", 0, params -> "Q" + (rnd.nextInt(4) + 1));
        engine.registerGenerator("year", 0, params -> String.valueOf(java.time.LocalDate.now().getYear()));
        engine.registerGenerator("priority", 0, params -> rnd.nextBoolean() ? "HIGH" : "NORM");
        engine.registerGenerator("buildingNumber", 0, params -> String.valueOf(rnd.nextInt(500) + 1));
        engine.registerGenerator("zip", 0, params -> String.format("%05d", rnd.nextInt(90000) + 10000));

        // 8. Custom composites utilizing Database Service with fallback
        engine.registerGenerator("name", 0, params -> {
            String first = dbService != null ? dbService.getRandomValue("firstNames", "", globalContext, null) : null;
            String last = dbService != null ? dbService.getRandomValue("lastNames", "", globalContext, null) : null;
            if (first == null || first.isEmpty()) first = dbService != null ? dbService.getRandomValue("firstName", "", globalContext, null) : null;
            if (last == null || last.isEmpty()) last = dbService != null ? dbService.getRandomValue("lastName", "", globalContext, null) : null;
            if (first == null || first.isEmpty()) first = "John";
            if (last == null || last.isEmpty()) last = "Doe";
            return first + " " + last;
        });

        engine.registerGenerator("email", 0, params -> {
            String first = dbService != null ? dbService.getRandomValue("firstNames", "", globalContext, null) : null;
            String last = dbService != null ? dbService.getRandomValue("lastNames", "", globalContext, null) : null;
            if (first == null || first.isEmpty()) first = dbService != null ? dbService.getRandomValue("firstName", "", globalContext, null) : null;
            if (last == null || last.isEmpty()) last = dbService != null ? dbService.getRandomValue("lastName", "", globalContext, null) : null;
            if (first == null || first.isEmpty()) first = "john";
            if (last == null || last.isEmpty()) last = "doe";
            return (first + "." + last + "@example.com").toLowerCase();
        });

        engine.registerGenerator("phone", 0, params -> "+" + rnd.nextInt(1, 99) + "-" + rnd.nextInt(100, 999) + "-" + rnd.nextInt(1000, 9999));
        engine.registerGenerator("birthDate", 0, params -> String.format("%04d-%02d-%02d", rnd.nextInt(1950, 2005), rnd.nextInt(1, 12), rnd.nextInt(1, 28)));

        engine.registerGenerator("addressLine", 0, params -> {
            String bldg = String.valueOf(rnd.nextInt(1, 500));
            String street = dbService != null ? dbService.getRandomValue("streets", "", globalContext, null) : null;
            String town = dbService != null ? dbService.getRandomValue("towns", "", globalContext, null) : null;
            if (street == null || street.isEmpty()) street = dbService != null ? dbService.getRandomValue("street", "", globalContext, null) : null;
            if (town == null || town.isEmpty()) town = dbService != null ? dbService.getRandomValue("town", "", globalContext, null) : null;
            if (street == null || street.isEmpty()) street = "Wall St";
            if (town == null || town.isEmpty()) town = "New York";
            return bldg + " " + street + ", " + town;
        });
    }

    private static String randomAlphanumeric(Random rnd, int length) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(chars.charAt(rnd.nextInt(chars.length())));
        }
        return sb.toString();
    }

    public String generate(String messageType) {
        if (messageType == null) return "Error: messageType is null";
        if (messageType.contains(",")) {
            String[] types = messageType.split(",");
            String chosen = types[new Random().nextInt(types.length)].trim();
            return generateSingle(chosen);
        }
        return generateSingle(messageType);
    }

    private String generateSingle(String messageType) {
        String template = discovery.getTemplate(messageType);
        if (template == null) {
            String target = messageType.toLowerCase().replaceAll("[^a-z0-9]", "");
            for (String name : discovery.getTemplateNames()) {
                if (name.toLowerCase().replaceAll("[^a-z0-9]", "").equals(target)) {
                    template = discovery.getTemplate(name);
                    break;
                }
            }
        }
        if (template == null) {
            System.err.println("[UniversalFaker] Could not find template for: " + messageType);
            return "No template found for: " + messageType;
        }
        return engine.process(template, globalContext.fork(), dbService);
    }

    public String generateDirect(String template) {
        return engine.process(template, globalContext.fork(), dbService);
    }

    public List<String> listTemplates() {
        return discovery.getTemplateNames();
    }

    public void reloadTemplates() {
        discovery.scanTemplates();
    }
}
