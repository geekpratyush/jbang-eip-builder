# UI Refactoring and Componentization Plan

## Findings & Current Technical Debt
The Route Builder Studio codebase contains several duplicated UI patterns across its various windows (`TransformationStudioWindow`, `ValidatorStudioWindow`, `FakerStudioWindow`, `MappingArchitectWindow`, `DiagramPane`, etc.). 

1.  **Button Creation:** Methods like `createSmallButton(icon, tooltip)` exist in almost every Window class.
2.  **Panel Headers:** The creation of an `HBox` with a label, spacer, and buttons is copy-pasted everywhere.
3.  **Monaco Editor Hosting:** The complex logic to spin up a local `HttpServer` and load a `WebView` with Monaco is duplicated in at least 4 files.
4.  **Theming:** Theming logic (`RouteBuilderApp.currentThemeClass`) is spread out. If a new stage is opened *after* a theme is changed, it picks up the current theme, but changing the theme while multiple windows are open requires looping over static instances.
5.  **Console Pane:** There is a `ConsolePane.java`, but we need to ensure it's used uniformly.

## Execution Tasks

### [X] Task 1: Create `ThemeManager`
- Centralize all theme definitions (Dark, Light, Hacker, etc.).
- Maintain a registry of active JavaFX `Scene` or `Parent` roots.
- Provide a `applyTheme(String themeName)` method that universally updates all registered windows.

### [X] Task 2: Create `UIFactory`
- Create a static factory class for generic UI components.
- Methods: `createIconBtn(icon, tooltip)`, `createPanelHeader(title, Node... actions)`.
- Apply this to `MappingArchitectWindow` and `TransformationStudioWindow` first.

### [X] Task 3: Create `MonacoEditorPane`
- Encapsulate the `WebView`, `WebEngine`, and `HttpServer` into a single reusable JavaFX Node.
- Expose methods: `setText(String)`, `getText()`, `setLanguage(String)`.
- Replace the raw WebViews in `TransformationStudioWindow` and `MappingArchitectWindow`.

### [X] Task 4: Standardize the `ConsolePane`
- Ensure `ConsolePane` exposes a static `log(String)` or is available globally.
- Standardize its look to match the "Hacker" or current theme.

## Execution Strategy
I will execute these tasks sequentially to ensure the application remains buildable and stable after each step.
