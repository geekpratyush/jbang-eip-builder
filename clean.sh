#!/bin/bash
# clean.sh - Clean all generated, compiled, and binary files to inspect true source size.

# Exit immediately if a command exits with a non-zero status
set -e

echo "=== Cleaning Gradle Build Artifacts ==="
if [ -f "./gradlew" ]; then
    ./gradlew clean || true
fi

echo "=== Deleting other build, target and cache directories ==="
find . -name "build" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name "target" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name ".gradle" -type d -exec rm -rf {} + 2>/dev/null || true
find . -name "node_modules" -type d -exec rm -rf {} + 2>/dev/null || true

echo "=== Deleting JBang & Camel runtime caches ==="
find . -name ".camel-jbang*" -type d -not -path "*/src/*" -exec rm -rf {} + 2>/dev/null || true
find . -name ".tessera" -type d -not -path "*/src/*" -exec rm -rf {} + 2>/dev/null || true

echo "=== Deleting Log files & lock files ==="
find . -name "*.log*" -type f -delete 2>/dev/null || true
find . -name "*.FDC" -type f -delete 2>/dev/null || true
find . -name "FFDC" -type d -exec rm -rf {} + 2>/dev/null || true

echo "=== Deleting generated environment and cert configurations ==="
rm -rf infra-setup/certs/
find . -name ".env" -type f -delete 2>/dev/null || true

echo "=== Deleting generated sandbox/workspace directories inside tessera-builder ==="
rm -rf tessera-builder/assets/
rm -rf tessera-builder/camel/
rm -rf tessera-builder/diagrams/
rm -rf tessera-builder/FAKER/
rm -rf tessera-builder/kamelets/
rm -rf tessera-builder/mappings/
rm -rf tessera-builder/schemas/
rm -rf tessera-builder/validator/
rm -rf tessera-builder/routes/
rm -f tessera-builder/validation-mapping.json
rm -f tessera-builder/application.properties

echo "=== Deleting large pre-compiled binaries (can be restored via git checkout) ==="
rm -f infra-setup/docker-compose
rm -f tessera-builder/lsp/camel-lsp-server.jar
rm -f db-update/liquibase-migrator-runner.jar

echo "=== Cleanup Complete! ==="
echo ""
echo "Current workspace directory size (excluding .git):"
du -sh --exclude=.git .
echo ""
echo "Note: If you need to restore the camel-lsp-server.jar and other files, you can run:"
echo "  git checkout -- tessera-builder/lsp/camel-lsp-server.jar"
echo "  git checkout -- infra-setup/docker-compose"
