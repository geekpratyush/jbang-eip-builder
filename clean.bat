@echo off
echo Cleaning up build and temporary files...

:: Root directories
rd /s /q .gradle 2>nul
rd /s /q build 2>nul

:: Subproject build directories
rd /s /q route-builder\build 2>nul
rd /s /q route-builder\.gradle 2>nul

:: IDE files
rd /s /q .idea 2>nul
rd /s /q .vscode 2>nul
del /f /q *.iml 2>nul
del /f /q *.ipr 2>nul
del /f /q *.iws 2>nul
del /f /q route-builder\*.iml 2>nul

:: Log files
del /f /q *.log 2>nul
del /f /q lsp_log*.txt 2>nul
del /f /q route-builder\*.log 2>nul

:: JBang & Camel JBang directories (recursive)
echo Removing JBang/Camel JBang cache directories...
rd /s /q .jbang 2>nul
rd /s /q .camel-jbang 2>nul
rd /s /q .camel-jbang-run 2>nul
for /d /r . %%d in (.jbang) do @if exist "%%d" rd /s /q "%%d" 2>nul
for /d /r . %%d in (.camel-jbang) do @if exist "%%d" rd /s /q "%%d" 2>nul
for /d /r . %%d in (.camel-jbang-run) do @if exist "%%d" rd /s /q "%%d" 2>nul

:: Temporary route files generated during execution
echo Removing temporary route yaml files...
del /s /f /q temp-*.camel.yaml 2>nul

:: Sandbox Workspace Directories
for /d %%d in (route-builder\chapter-*) do rd /s /q "%%d" 2>nul
rd /s /q route-builder\infra-simulator 2>nul
rd /s /q route-builder\FAKER 2>nul
rd /s /q route-builder\validator-workspace 2>nul
rd /s /q validator-workspace 2>nul
del /f /q route-builder\application.properties 2>nul
rd /s /q test-project-workspace\FAKER 2>nul
rd /s /q test-project-workspace\infra-simulator 2>nul
rd /s /q infra-simulator 2>nul

echo Cleanup completed successfully!
