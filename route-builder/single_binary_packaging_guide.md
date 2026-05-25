# Self-Contained Route Builder Packaging & Distribution Guide

This guide describes how to bundle the **Camel Quarkus Route Builder IDE** with JBang, the Language Server, sample routes, and documentation into a single, ready-to-run package for developers.

---

## 1. Distribution Architecture

To avoid asking users to download JBang, configure paths, or install Java dependencies separately, we have modified the build configuration and code to use **install-relative path resolution**:

```
route-builder-1.0-SNAPSHOT/
├── bin/
│   ├── route-builder         <-- Linux/macOS Shell Script (runs JavaFX App)
│   └── route-builder.bat     <-- Windows Batch Script (runs JavaFX App)
├── lib/
│   ├── route-builder-1.0-SNAPSHOT.jar  <-- Core JavaFX Application
│   └── *.jar                 <-- JavaFX, RichTextFX, LSP4j, Commonmark dependencies
├── lsp/
│   └── camel-lsp-server.jar  <-- Bundled Camel Language Server (LSP)
├── routes/
│   └── sample-project/       <-- Pre-packaged sample routes, Groovy, and XSLTs
├── .jbang/
│   └── jbang.jar             <-- Core offline JBang runtime
├── jbang                     <-- Unix JBang execution wrapper
├── jbang.cmd                 <-- Windows JBang execution wrapper
├── jbang.ps1                 <-- PowerShell JBang wrapper
└── User Manual.md            <-- Offline HTML help viewer document
```

---

## 2. Robust Install-Relative Resolution

In `RouteBuilderApp.java` and `LspManager.java`, we resolve local execution assets relative to the **installation directory** of the running JAR rather than relying on the user's terminal working directory (`user.dir`):

### JBang Resolution Logic
```java
private String getJbangExecutable() {
    String os = System.getProperty("os.name").toLowerCase();
    String jbangScript = os.contains("win") ? "jbang.cmd" : "jbang";
    java.io.File jbangExe = null;
    try {
        // Resolve path to the running JAR file:
        java.io.File jarFile = new java.io.File(RouteBuilderApp.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        // Since jar is in lib/ (e.g. /app/lib/app.jar), its parent's parent is the installation root:
        java.io.File installDir = jarFile.getParentFile().getParentFile();
        jbangExe = new java.io.File(installDir, jbangScript);
    } catch (Exception ignored) {}
    
    // Fall back to working directory if running in developer mode (unpackaged IDE run):
    if (jbangExe == null || !jbangExe.exists()) {
        jbangExe = new java.io.File(System.getProperty("user.dir"), jbangScript);
    }
    return jbangExe.exists() ? jbangExe.getAbsolutePath() : "jbang";
}
```

### Language Server (LSP) Resolution Logic
```java
File lspJar = null;
try {
    File jarFile = new File(LspManager.class.getProtectionDomain().getCodeSource().getLocation().toURI());
    File installDir = jarFile.getParentFile().getParentFile();
    lspJar = new File(installDir, "lsp/camel-lsp-server.jar");
} catch (Exception ignored) {}

if (lspJar == null || !lspJar.exists()) {
    lspJar = new File("lsp/camel-lsp-server.jar");
}
```

---

## 3. Creating the Distribution Package

You can compile, build, and package the entire self-contained workspace into a single zip/tar distribution with one command:

```bash
./gradlew distZip
```

This creates the packaged zip file:
`build/distributions/route-builder-1.0-SNAPSHOT.zip`

### Distributing to Users
Provide this `.zip` file directly to the user. To use the application, they only need to:
1. **Unzip** the archive to any directory.
2. **Execute** the start script:
   - On Linux/macOS: `./bin/route-builder`
   - On Windows: double-click `bin/route-builder.bat`

---

## 4. Packaging as a Native Native Executable/Installer (`jpackage`)

If you want to go a step further and bundle a **Private Java Runtime Environment (JRE)** so the user doesn't even need to have Java 21 pre-installed on their system, you can use the official JDK `jpackage` tool:

### Packaging command (Linux example):
```bash
# 1. Compile the project classes and copy dependencies into build/install
./gradlew installDist

# 2. Package into a platform-specific application directory/installer:
jpackage \
  --name "RouteBuilderStudio" \
  --input build/install/route-builder/lib \
  --main-jar route-builder-1.0-SNAPSHOT.jar \
  --main-class com.routebuilder.Main \
  --type app-image \
  --dest build/dist-native \
  --icon src/main/resources/app-icon.png
```

This will output a native folder structure under `build/dist-native/RouteBuilderStudio` containing its own embedded JVM. Copy the `jbang`, `lsp`, and `routes` folders into that native folder and distribute it as a fully self-contained desktop package.
