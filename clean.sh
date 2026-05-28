#!/bin/bash

echo "Cleaning up build and temporary files..."

# Root directories
rm -rf .gradle
rm -rf build

# Subproject build directories
rm -rf route-builder/build
rm -rf route-builder/.gradle

# IDE files
rm -rf .idea
rm -rf .vscode
rm -f *.iml
rm -f *.ipr
rm -f *.iws
rm -f route-builder/*.iml

# Log files
rm -f *.log
rm -f lsp_log*.txt
rm -f route-builder/*.log

# JBang & Camel JBang directories (recursive search)
echo "Removing JBang/Camel JBang cache directories..."
rm -rf .jbang
rm -rf .camel-jbang
rm -rf .camel-jbang-run
find . -type d -name ".jbang" -exec rm -rf {} + 2>/dev/null
find . -type d -name ".camel-jbang" -exec rm -rf {} + 2>/dev/null
find . -type d -name ".camel-jbang-run" -exec rm -rf {} + 2>/dev/null

# Temporary route files generated during execution
echo "Removing temporary route yaml files..."
find . -type f -name "temp-*.camel.yaml" -delete 2>/dev/null

# Sandbox Workspace Directories
rm -rf route-builder/chapter-*
rm -rf route-builder/infra-simulator
rm -rf route-builder/FAKER
rm -rf route-builder/validator-workspace
rm -rf validator-workspace
rm -f route-builder/application.properties
rm -rf test-project-workspace/FAKER
rm -rf test-project-workspace/infra-simulator
rm -rf infra-simulator

echo "Cleanup completed successfully!"
