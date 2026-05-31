# Project Explorer & Database Export

The Project Explorer provides file management and batch command execution hooks to speed up Camel Quarkus project setup and deployment.

---

## 1. Explorer Capabilities

- **Hierarchical Directory Tree**: Lists directories, subdirectories, routes, and Kamelet templates recursively.
- **Advanced Drag & Drop**: Mass-movement and mass-copying of multiple files or folders simultaneously.
- **Auto-Suffix Renaming**: Preventing accidental data loss during file copies by appending numeric suffixes (`_1`, `_2`).
- **Batch Command Execution**: Context menus allowing recursive running of selected routes/directories.

---

## 2. JBang Runtime Profiles

When launching a Camel route, developers can run under two execution profiles:
- **Offline Stub Mode**: Runs routes using stub components for external connections.
- **Live Mode**: Standard JBang run connecting to live message queues, databases, and external endpoints.

---

## 3. Liquibase Database Exports

For databases, the IDE exports route tables and definitions as schema changelogs:
- **SQL Table Mapping**: Converts SQL databases and definitions into Liquibase XML/YAML changelogs.
- **MongoDB Collection Mapping**: Translates JSON documents and database collections into MongoDB-specific Liquibase changelogs.
